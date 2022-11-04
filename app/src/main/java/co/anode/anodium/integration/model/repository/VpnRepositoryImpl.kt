package co.anode.anodium.integration.model.repository

import android.content.Context
import android.content.Intent
import co.anode.anodium.AnodeVpnService
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.CjdnsSocket
import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnState
import com.pkt.domain.interfaces.VpnAPIService
import com.pkt.domain.repository.VpnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepositoryImpl @Inject constructor() : VpnRepository {
    private val defaultNode = "929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k"
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

    private val _currentVpnNameFlow: MutableStateFlow<String?> by lazy { MutableStateFlow(null) }
    override val currentVpnFlow: Flow<Vpn?>
        get() = combine(
            vpnListFlow,
            _currentVpnNameFlow
        ) { vpnList, currentVpn ->
            vpnList.find { it.name == currentVpn } ?: vpnList.firstOrNull()
        }

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

    override suspend fun setCurrentVpn(name: String): Result<Unit> {
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putString("LastServerPubkey", defaultNode)?.apply()
        return Result.success(Unit)
    }

    override suspend fun connect(node: String): Result<Boolean>  {
        _vpnState.tryEmit(VpnState.CONNECTING)
        AnodeUtil.addCjdnsPeers()

        if(authorizeVPN().isSuccess){
            _vpnState.tryEmit(VpnState.CONNECTED)
            val connectedNode = AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("ServerPublicKey","")
            if ((!node.isNullOrEmpty()) && ((!AnodeClient.isVpnActive()) || (node != connectedNode))) {
                AnodeClient.cjdnsConnectVPN(node)
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

    override suspend fun authorizeVPN(): Result<Boolean> {
        val jsonObject = JSONObject()
        val date = System.currentTimeMillis()
        jsonObject.accumulate("date", date)
        val bytes = jsonObject.toString().toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest: ByteArray = md.digest(bytes)
        val digestStr = Base64.getEncoder().encodeToString(digest)
        val res = CjdnsSocket.Sign_sign(digestStr)
        val sig = res["signature"].str()
        val pubKey = AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("LastServerPubkey", defaultNode)
        if (pubKey != null) {
            vpnAPI.authorizeVPN(sig, pubKey, date)
        } else {
            vpnAPI.authorizeVPN(sig, defaultNode, date)
        }

        return Result.success(true)
    }

    override suspend fun disconnect(): Result<Boolean> {
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
