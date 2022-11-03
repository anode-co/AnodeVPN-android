package co.anode.anodium.integration.model.repository

import android.content.Context
import co.anode.anodium.BuildConfig
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.CjdnsSocket
import com.pkt.domain.dto.CjdnsConnection
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.CjdnsPeer
import com.pkt.domain.repository.CjdnsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CjdnsRepositoryImpl @Inject constructor() : CjdnsRepository {
    override suspend fun getCjdnsRoutes(): Boolean {
        return CjdnsSocket.getCjdnsRoutes()
    }

    override suspend fun getCjdnsPeers(): List<CjdnsPeer> {
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
        return result
    }

    override suspend fun getCjdnsConnections(): List<CjdnsConnection> {
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
        return result
    }

    override suspend fun getCjdnsInfo(): Result<CjdnsInfo> {
        val ipv4 = CjdnsSocket.ipv4Address
        val cjdnsNodeInfo = CjdnsSocket.Core_nodeInfo()
        val ipv6 = cjdnsNodeInfo["myIp6"].str()
        val internetipv6 = CjdnsSocket.ipv6Address
        val cjdnsConnection = getCjdnsConnections()
        val cjdnsPeers = getCjdnsPeers()
        val key = AnodeUtil.getPubKey()
        var username = ""
        var vpnExit = ""
        val prefs = AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)
        if (prefs != null) {
            username = prefs.getString("username", "")!!
            vpnExit = prefs.getString("ServerPublicKey", "")!!
        }

        val nodeUrl = "http://h.snode.cjd.li/#" + cjdnsNodeInfo["myIp6"].str()
        return Result.success(CjdnsInfo(BuildConfig.VERSION_NAME, ipv4,ipv6,internetipv6,cjdnsConnection[0],cjdnsPeers,key,username,nodeUrl,vpnExit))
    }

}