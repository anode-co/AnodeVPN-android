package co.anode.anodium.integration.model.repository

import android.content.Context
import android.content.Intent
import co.anode.anodium.AnodeVpnService
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.CjdnsSocket
import com.pkt.domain.dto.CjdnsPeeringLine
import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnState
import com.pkt.domain.interfaces.VpnAPIService
import com.pkt.domain.repository.VpnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepositoryImpl @Inject constructor() : VpnRepository {
    private val defaultNode = "1y7k7zb64f242hvv8mht54ssvgcqdfzbxrng5uz7qpgu7fkjudd0.k"
    private val vpnAPI = VpnAPIService()
    private val _vpnState: MutableStateFlow<VpnState> by lazy { MutableStateFlow(VpnState.DISCONNECTED) }

    override val vpnStateFlow: Flow<VpnState>
        get() = _vpnState

    private var _startConnectionTime: Long = 0L
    override val startConnectionTime: Long
        get() = _startConnectionTime

    private val _vpnListFlow: MutableStateFlow<List<Vpn>> by lazy { MutableStateFlow(emptyList()) }
    override val vpnListFlow: Flow<List<Vpn>>
        get() = _vpnListFlow

    private val _currentVpnFlow: MutableStateFlow<Vpn> by lazy { MutableStateFlow(Vpn("goofy14-vpn.anode.co","CA","929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k")) }
    override val currentVpnFlow: Flow<Vpn>
        get() = _currentVpnFlow

    override suspend fun fetchVpnList(force: Boolean): Result<List<Vpn>> {
        val list = vpnAPI.getVpnServersList()

        val vpnList: MutableList<Vpn> = mutableListOf()
        for(i in list.indices){
            val server = list[i]
            vpnList.add(Vpn(server.name, server.countryCode, server.publicKey))
        }
        _vpnListFlow.tryEmit(vpnList)
        return Result.success(vpnList)
    }

    override suspend fun setCurrentVpn(vpn: Vpn): Result<Unit> {
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putString("LastServerPubkey", vpn.publicKey)?.apply()
        _currentVpnFlow.tryEmit(vpn)
        return Result.success(Unit)
    }

    fun cjdnsConnectVPN(node: String) {
        if (!AnodeUtil.internetConnection()) {
            _vpnState.tryEmit(VpnState.NO_INTERNET)
            return
        }
        var iconnected = false
        CjdnsSocket.IpTunnel_removeAllConnections()
        if (node.isNotEmpty()) {
            //Connect to Internet
            CjdnsSocket.ipTunnelConnectTo(node)
        }
        var tries = 0
        //Check for ip address given by cjdns try for 20 times, 10secs
        Thread(Runnable {
            while ((node.isNotEmpty()) && (!iconnected && (tries < 10))) {
                _startConnectionTime = 0L
                _vpnState.tryEmit(VpnState.GETTING_ROUTES)
                iconnected = CjdnsSocket.getCjdnsRoutes()
                tries++
                Thread.sleep(2000)
            }
            if (iconnected || node.isEmpty()) {
                _vpnState.tryEmit(VpnState.GOT_ROUTES)
                //Restart Service
                CjdnsSocket.Core_stopTun()
                AnodeClient.mycontext.startService(Intent(AnodeClient.mycontext, AnodeVpnService::class.java).setAction("co.anode.anodium.DISCONNECT"))
                AnodeClient.mycontext.startService(Intent(AnodeClient.mycontext, AnodeVpnService::class.java).setAction("co.anode.anodium.START"))
                _startConnectionTime = System.currentTimeMillis()
                _vpnState.tryEmit(VpnState.CONNECTED)
                //Start Thread for checking connection
                //AnodeClient.h.postDelayed(AnodeClient.runnableConnection, 10000)
            } else {
                CjdnsSocket.IpTunnel_removeAllConnections()
                _startConnectionTime = 0L
                _vpnState.tryEmit(VpnState.DISCONNECTED)
                //Stop UI thread
                //AnodeClient.h.removeCallbacks(AnodeClient.runnableConnection)
            }
        }, "VpnRepository.cjdnsConnectVPN").start()
    }

    override suspend fun connect(node: String): Result<Boolean>  {
        _vpnState.tryEmit(VpnState.CONNECTING)
        AnodeUtil.addCjdnsPeers()
        if(authorizeVPN().isSuccess){
            val connectedNode = AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("ServerPublicKey","")
            if ((!node.isNullOrEmpty()) && ((!AnodeClient.isVpnActive()) || (node != connectedNode))) {
                cjdnsConnectVPN(node)
            }
            AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putString("ServerPublicKey", node)?.apply()
            return Result.success(true)
        } else {
            _vpnState.tryEmit(VpnState.DISCONNECTED)
            CjdnsSocket.IpTunnel_removeAllConnections()
            CjdnsSocket.Core_stopTun()
            CjdnsSocket.clearRoutes()
            AnodeUtil.context?.startService(Intent(AnodeUtil.context, AnodeVpnService::class.java).setAction("co.anode.anodium.DISCONNECT"))
            return Result.success(false)
        }
    }

    fun getCjdnsSignature(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest: ByteArray = md.digest(bytes)
        val digestStr = Base64.getEncoder().encodeToString(digest)
        val res = CjdnsSocket.Sign_sign(digestStr)
        return res["signature"].str()
    }

    override suspend fun authorizeVPN(): Result<Boolean> {
        val date = System.currentTimeMillis()
        val jsonObject = JSONObject()
        jsonObject.accumulate("date", date)
        val sig = getCjdnsSignature(jsonObject.toString().toByteArray())
        val pubKey = AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("LastServerPubkey", defaultNode)
        if (pubKey != null) {
            vpnAPI.authorizeVPN(sig, pubKey, date)
        } else {
            vpnAPI.authorizeVPN(sig, defaultNode, date)
        }

        return Result.success(true)
    }

    override suspend fun getCjdnsPeers(): Result<List<CjdnsPeeringLine>> {
        return vpnAPI.getCjdnsPeeringLines()
    }

    override fun postError(error: String): Result<String> {
        return vpnAPI.postError(error)
    }

    override suspend fun generateUsername(): Result<String> {
        val signature = getCjdnsSignature("".toByteArray())
        runCatching {
            vpnAPI.generateUsername(signature).getOrThrow()
        }.onSuccess { username ->
            setUsername(username)
            return Result.success(username)
        }.onFailure {
            return Result.failure(it)
        }

        return Result.success("")
    }

    override fun setUsername(username: String) {
        AnodeUtil.setUsernameToSharedPrefs(username)
    }

    override fun getUsername(): String {
        return AnodeUtil.getUsernameFromSharedPrefs()
    }

    override suspend fun disconnect(): Result<Boolean> {
        _startConnectionTime = 0L
        _vpnState.tryEmit(VpnState.DISCONNECTED)
        AnodeClient.AuthorizeVPN().cancel(true)
        AnodeClient.stopThreads()
        CjdnsSocket.IpTunnel_removeAllConnections()
        CjdnsSocket.Core_stopTun()
        CjdnsSocket.clearRoutes()
        AnodeUtil.context?.startService(Intent(AnodeUtil.context, AnodeVpnService::class.java).setAction("co.anode.anodium.STOP"))
        //bigbuttonState(buttonStateDisconnected)

        //Rating bar
        /*Removed rating*/
        /*if (showRatingBar) {
            val ratingFragment: BottomSheetDialogFragment = RatingFragment()
            activity?.let { ratingFragment.show(it.supportFragmentManager, "") }
        }*/
        return Result.success(true)
    }

    override suspend fun getIPv4Address(): Result<String> {
        return Result.success(vpnAPI.getIPv4Address())
    }

    override suspend fun getIPv6Address(): Result<String> {
        return Result.success(vpnAPI.getIPv6Address())
    }


}
