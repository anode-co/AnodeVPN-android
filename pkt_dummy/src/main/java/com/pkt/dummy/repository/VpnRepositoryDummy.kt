package com.pkt.dummy.repository

import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnServer
import com.pkt.domain.dto.VpnState
import com.pkt.domain.repository.VpnRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class VpnRepositoryDummy : VpnRepository {

    private val _currentVpnNameFlow: MutableStateFlow<String?> by lazy { MutableStateFlow(null) }

    private val _vpnListFlow: MutableStateFlow<List<Vpn>> by lazy { MutableStateFlow(emptyList()) }
    override val vpnListFlow: Flow<List<Vpn>>
        get() = _vpnListFlow

    override val currentVpnFlow: Flow<Vpn?>
        get() = combine(
            vpnListFlow,
            _currentVpnNameFlow
        ) { vpnList, currentVpn ->
            vpnList.find { it.name == currentVpn } ?: vpnList.firstOrNull()
        }

    private val _vpnState: MutableStateFlow<VpnState> by lazy { MutableStateFlow(VpnState.DISCONNECTED) }
    override val vpnStateFlow: Flow<VpnState>
        get() = _vpnState

    private var _startConnectionTime: Long = 0L
    override val startConnectionTime: Long
        get() = _startConnectionTime

    override suspend fun fetchVpnList(force: Boolean): Result<List<Vpn>> {
        if (_vpnListFlow.value.isEmpty() || force) {
            delay(1000L)
            _vpnListFlow.tryEmit(VPN_LIST)
        }
        return Result.success(listOf())
    }

    override suspend fun setCurrentVpn(name: String) :Result <Unit>{
        _currentVpnNameFlow.tryEmit(name)
        return Result.success(Unit)
    }

    override suspend fun connect(node: String): Result<Boolean> {
        _vpnState.tryEmit(VpnState.CONNECTING)
        delay(1000L)
        _startConnectionTime = System.currentTimeMillis()
        _vpnState.tryEmit(VpnState.CONNECTED)
        return Result.success(true)
    }

    override suspend fun disconnect(): Result<Boolean> {
        _startConnectionTime = 0L
        _vpnState.tryEmit(VpnState.DISCONNECTED)
        return Result.success(true)
    }

    override suspend fun getIPv4Address(): Result<String> {
        return Result.success("192.168.1.1")
    }

    override suspend fun getIPv6Address(): Result<String> {
        return Result.success("fc00::1")
    }

    override suspend fun authorizeVPN(): Result<Boolean> {
        TODO("Not yet implemented")
    }

    private companion object {
        val VPN_LIST = listOf(
            Vpn(
                name = "goofy14-vpn.anode.co",
                countryCode = "CA",
                pubKey = "929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k"
            ),
            Vpn(
                name = "2022-virtual.anode.co",
                countryCode = "US",
                pubKey = "929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k"
            ),
        )
    }
}
