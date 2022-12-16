package com.pkt.domain.repository

import com.pkt.domain.dto.CjdnsPeeringLine
import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnServer
import com.pkt.domain.dto.VpnState
import kotlinx.coroutines.flow.Flow

interface VpnRepository {

    val vpnListFlow: Flow<List<Vpn>>
    val currentVpnFlow: Flow<Vpn?>
    val vpnStateFlow: Flow<VpnState>

    val startConnectionTime: Long

    suspend fun fetchVpnList(force: Boolean = false): Result<List<Vpn>>
    fun setCurrentVpn(vpn: Vpn): Result<Unit>
    fun connectFromExits(vpn: Vpn)
    suspend fun connect(node:String): Result<Boolean>
    suspend fun disconnect(): Result<Boolean>
    suspend fun getIPv4Address(): Result<String>
    suspend fun getIPv6Address(): Result<String>
    suspend fun authorizeVPN(): Result<Boolean>
    suspend fun getCjdnsPeers(): Result<List<CjdnsPeeringLine>>
    fun postError(error: String): Result<String>
    suspend fun generateUsername(): Result<String>
    fun setUsername(username: String)
    fun getUsername(): String
}
