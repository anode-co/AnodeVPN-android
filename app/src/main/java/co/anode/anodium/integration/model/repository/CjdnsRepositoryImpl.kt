package co.anode.anodium.integration.model.repository

import android.content.Context
import co.anode.anodium.BuildConfig
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.CjdnsSocket
import com.pkt.domain.dto.CjdnsConnection
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.CjdnsPeer
import com.pkt.domain.dto.CjdnsPeeringLine
import com.pkt.domain.repository.CjdnsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CjdnsRepositoryImpl @Inject constructor() : CjdnsRepository {
    override suspend fun getCjdnsRoutes(): Boolean {
        return CjdnsSocket.getCjdnsRoutes()
    }

    override suspend fun getCjdnsPeers(): Result<List<CjdnsPeer>> {
        val peers = CjdnsSocket.InterfaceController_peerStats()
        val result: MutableList<CjdnsPeer> = mutableListOf()
        for (i in 0 until peers.size) {
            val ipv4 = peers[i]["lladdr"].toString().split(":")[0].trim('"')
            val port = peers[i]["lladdr"].toString().split(":")[1].trim('"').toInt()
            val key = peers[i]["addr"].toString()
            val status = peers[i]["state"].toString()
            val bytesIn: Long = peers[i]["recvKbps"].toString().trim('"').toLong()
            val bytesOut: Long = peers[i]["sendKbps"].toString().trim('"').toLong()
            val bytesLost: Long = 0
            result.add(CjdnsPeer(ipv4,port,key,status,bytesIn,bytesOut,bytesLost))
        }
        return Result.success(result)
    }

    override suspend fun getCjdnsConnections(): Result<List<CjdnsConnection>> {
        val list = CjdnsSocket.IpTunnel_listConnections()
        var result: MutableList<CjdnsConnection> = mutableListOf()
        for(i in 0 until list.size) {
            val ipv4Address = list[i]["ip4Address"].toString().trim('"')
            val ipv6Address = list[i]["ip6Address"].toString().trim('"')
            val ipv4Alloc = list[i]["ip4Alloc"].toString().trim('"').toInt()
            val key = list[i]["key"].toString().trim('"')
            val error = list[i]["error"].toString().trim('"')
            val ipv6Alloc = list[i]["ip6Alloc"].toString().trim('"').toInt()
            val ipv4Prefix = list[i]["ip4Prefix"].toString().trim('"').toInt()
            val ipv6Prefix = list[i]["ip6Prefix"].toString().trim('"').toInt()
            val outgoing = list[i]["outgoing"].toString().trim('"').toInt()
            result.add(CjdnsConnection(ipv4Address, ipv6Address, key, ipv4Alloc, error, ipv6Alloc, ipv4Prefix, ipv6Prefix, outgoing))
        }
        return Result.success(result)
    }

    override suspend fun addCjdnsPeers(peers: List<CjdnsPeeringLine>): Boolean {
        try {
            for (i in peers.indices) {
                CjdnsSocket.UDPInterface_beginConnection(peers[i].publicKey, peers[i].ip, peers[i].port, peers[i].password, peers[i].login)
            }
            return true
        }catch (e: Exception) {
            return false
        }
    }

    override suspend fun getCjdnsInfo(): Result<CjdnsInfo> = withContext(Dispatchers.IO) {
        val ipv4 = CjdnsSocket.ipv4Address
        val cjdnsNodeInfo = CjdnsSocket.Core_nodeInfo()
        val ipv6 = cjdnsNodeInfo["myIp6"].str()
        val internetipv6 = CjdnsSocket.ipv6Address
        var cjdnsConnection = CjdnsConnection("","","",0,"",0,0,0,0)
        var cjdnsPeers: MutableList<CjdnsPeer> = mutableListOf()
        runCatching {
            val connection = getCjdnsConnections().getOrThrow()
            val peers = getCjdnsPeers().getOrThrow()
            Pair(connection, peers)
        }.onSuccess {(connection, peers) ->
            if(connection.isNotEmpty()) {
                cjdnsConnection = connection[0]
            }
            cjdnsPeers = peers.toMutableList()
        }.onFailure {
            cjdnsConnection = CjdnsConnection("","","",0,"",0,0,0,0)
        }

        val key = AnodeUtil.getPubKey()
        var username = ""
        var vpnExit = ""
        val prefs = AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)
        if (prefs != null) {
            username = prefs.getString("username", "")!!
            vpnExit = prefs.getString("ServerPublicKey", "")!!
        }

        val nodeUrl = "http://h.snode.cjd.li/#" + cjdnsNodeInfo["myIp6"].str()
        val response = CjdnsInfo(BuildConfig.VERSION_NAME, ipv4,ipv6,internetipv6,cjdnsConnection,cjdnsPeers,key,username,nodeUrl,vpnExit)
        Result.success(response)
    }

}