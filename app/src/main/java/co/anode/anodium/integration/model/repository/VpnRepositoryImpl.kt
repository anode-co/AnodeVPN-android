package co.anode.anodium.integration.model.repository

import android.content.Context
import android.content.Intent
import android.os.Handler
import co.anode.anodium.AnodeVpnService
import co.anode.anodium.BuildConfig
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.CjdnsSocket
import com.pkt.domain.dto.CjdnsPeeringLine
import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnState
import com.pkt.domain.interfaces.VpnAPIService
import com.pkt.domain.repository.VpnRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import timber.log.Timber
import java.lang.Runnable
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepositoryImpl @Inject constructor() : VpnRepository {
    private val defaultNode = "1y7k7zb64f242hvv8mht54ssvgcqdfzbxrng5uz7qpgu7fkjudd0.k"
    private val vpnAPI = VpnAPIService()
    private val _vpnState: MutableStateFlow<VpnState> by lazy { MutableStateFlow(VpnState.DISCONNECTED) }

    override val vpnStateFlow: Flow<VpnState>
        get() = _vpnState

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    private var _startConnectionTime: Long = 0L
    override val startConnectionTime: Long
        get() = _startConnectionTime

    private val _vpnListFlow: MutableStateFlow<List<Vpn>> by lazy { MutableStateFlow(emptyList()) }
    override val vpnListFlow: Flow<List<Vpn>>
        get() = _vpnListFlow

    private val _currentVpnFlow: MutableStateFlow<Vpn> by lazy { MutableStateFlow(Vpn("PKT Pal Montreal","CA","929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k")) }
    override val currentVpnFlow: Flow<Vpn>
        get() = _currentVpnFlow

    private val pollingCjdnsInterval = 10L //10sec polling IPs
    private var cjdnsPollingJob: ScheduledFuture<*>? = null

    private val handler = Handler()

    override suspend fun fetchVpnList(force: Boolean): Result<List<Vpn>> {
        Timber.d("VpnRepositoryImpl fetchVpnList")
            vpnAPI.getVpnServersList().onSuccess { list ->
            val vpnList: MutableList<Vpn> = mutableListOf()
            for(i in list.indices){
                val server = list[i]
                vpnList.add(Vpn(server.name, server.countryCode, server.publicKey))
            }
            _vpnListFlow.tryEmit(vpnList)
            return Result.success(vpnList)
        }.onFailure {
            Timber.e("VpnRepositoryImpl fetchVpnList failed")
            return Result.failure(it)
        }
        return Result.success(emptyList())
    }

    override fun setCurrentVpn(vpn: Vpn): Result<Unit> {
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putString("LastServerPubkey", vpn.publicKey)?.apply()
        _currentVpnFlow.tryEmit(vpn)
        return Result.success(Unit)
    }

    override fun connectFromExits(vpn: Vpn) {
        Timber.d("VpnRepositoryImpl connectFromExits")
        setCurrentVpn(vpn).getOrNull()
        _vpnState.tryEmit(VpnState.CONNECT)
    }

    fun cjdnsConnectVPN(node: String) {
        Timber.d("VpnRepositoryImpl cjdnsConnectVPN")
        if (!AnodeUtil.internetConnection()) {
            Timber.d("cjdnsConnectVPN: No internet connection")
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
                startCjdnsPolling()
            } else {
                CjdnsSocket.IpTunnel_removeAllConnections()
                _startConnectionTime = 0L
                _vpnState.tryEmit(VpnState.DISCONNECTED)
                //Stop UI thread
                stopCjdnsPolling()
            }
        }, "VpnRepository.cjdnsConnectVPN").start()
    }

    override suspend fun connect(node: String): Result<Boolean>  {
        Timber.d("VpnRepositoryImpl connect")
        if ((_vpnState.value == VpnState.CONNECTING) || (_vpnState.value == VpnState.CONNECTED)) {
            Timber.d("VpnRepositoryImpl connect: disconnect current before attempting to connect again")
            disconnect()
        }

        _vpnState.tryEmit(VpnState.CONNECTING)
        AnodeUtil.addCjdnsPeers()
        if(authorizeVPN().isSuccess){
            Timber.d("authorizeVPN success")
            val connectedNode = AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("ServerPublicKey","")
            if ((!node.isNullOrEmpty()) && ((!AnodeClient.isVpnActive()) || (node != connectedNode))) {
                cjdnsConnectVPN(node)
            }
            setLastConnectedVPN(node)
            return Result.success(true)
        } else {
            Timber.d("authorizeVPN failed")
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
        val pubKey = AnodeUtil.getLastServerPubkeyFromSharedPrefs()
        if (pubKey.isNotEmpty()) {
            Timber.d("authorizeVPN: pubKey: $pubKey")
            vpnAPI.authorizeVPN(sig, pubKey, date)
        } else {
            Timber.d("authorizeVPN: pubKey with defaultNode $defaultNode")
            vpnAPI.authorizeVPN(sig, defaultNode, date)
        }

        return Result.success(true)
    }

    override suspend fun getCjdnsPeers(): Result<List<CjdnsPeeringLine>> {
        Timber.d("VpnRepositoryImpl getCjdnsPeers")
        return vpnAPI.getCjdnsPeeringLines()
    }

    override fun postError(error: String): Result<String> {
        return vpnAPI.postError(error)
    }

    override suspend fun generateUsername(): Result<String> {
        Timber.d("VpnRepositoryImpl generateUsername")
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
        Timber.d("VpnRepositoryImpl setUsername")
        AnodeUtil.setUsernameToSharedPrefs(username)
    }

    override fun getUsername(): String {
        Timber.d("VpnRepositoryImpl getUsername")
        return AnodeUtil.getUsernameFromSharedPrefs()
    }

    override suspend fun disconnect(): Result<Boolean> {
        Timber.d("VpnRepositoryImpl disconnect")
        _startConnectionTime = 0L
        _vpnState.tryEmit(VpnState.DISCONNECTED)
        AnodeClient.AuthorizeVPN().cancel(true)
        stopCjdnsPolling()
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

    fun startCjdnsPolling() {
        stopCjdnsPolling()
        AnodeClient.runnableConnection.init(handler)
        handler.postDelayed(AnodeClient.runnableConnection, 10000)
        //cjdnsPollingJob = scheduler.scheduleAtFixedRate(pollintTask, 0, pollingCjdnsInterval, TimeUnit.SECONDS)
    }
    private fun stopCjdnsPolling() {
        handler.removeCallbacks(AnodeClient.runnableConnection)
    }

    //function to store the last connected node
    private fun setLastConnectedVPN(node: String) {
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putString("ServerPublicKey", node)?.apply()
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putLong("LastAuthorized", System.currentTimeMillis())?.apply()
    }

    override fun getLastConnectedVPN(): String {
        return AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("ServerPublicKey","") ?: ""
    }
}
