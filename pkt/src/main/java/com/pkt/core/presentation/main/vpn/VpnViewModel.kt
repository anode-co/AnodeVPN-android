package com.pkt.core.presentation.main.vpn

import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.dto.Vpn
import com.pkt.domain.repository.VpnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VpnViewModel @Inject constructor(
    private val vpnRepository: VpnRepository,
) : StateViewModel<VpnState>() {

    private val _timerUiState: MutableStateFlow<Int> by lazy { MutableStateFlow(0) }
    val timerUiState: Flow<Int> by lazy { _timerUiState }

    private var timerJob: Job? = null

    init {
        invokeLoadingAction {
            runCatching {
                val ipv4 = vpnRepository.getIPv4Address().getOrNull()
                val ipv6 = vpnRepository.getIPv6Address().getOrNull()
                val list = vpnRepository.fetchVpnList().getOrThrow()
                //Set default VPN
                var vpn = Vpn("Anode","US","929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k")
                //Check list for default VPN name
                if (list.isNotEmpty()) {
                    for (item in list) {
                        if (item.name == "2022-virtual.anode.co") {
                            vpn = item
                            break
                        }
                    }
                }
                Triple(ipv4,ipv6, vpn)
            }.onSuccess { (ipv4, ipv6, vpn) ->
                sendState {
                    copy(
                        vpn = vpn,
                        ipV4 = ipv4,
                        ipV6 = ipv6,
                    )
                }
            }
        }
//        sendEvent(VpnEvent.OpenConsent)

        viewModelScope.launch {
            combine(
                vpnRepository.currentVpnFlow,
                vpnRepository.vpnStateFlow
            ) { vpn, vpnState ->
                sendState {
                    copy(
                        vpn = vpn,
                        vpnState = vpnState
                    )
                }

                if (vpnState == com.pkt.domain.dto.VpnState.CONNECTED) {
                    _timerUiState.update {
                        ((System.currentTimeMillis() - vpnRepository.startConnectionTime) / 1000).toInt()
                    }

                    timerJob?.cancel()
                    timerJob = viewModelScope.launch {
                        while (isActive) {
                            delay(1_000L)
                            _timerUiState.update { it + 1 }
                        }
                    }
                } else {
                    _timerUiState.update { 0 }

                    timerJob?.cancel()
                }
            }.collect()
        }

        //createLoadingAction()
    }

    override fun createInitialState() = VpnState(
        ipV4 = "",
        ipV6 = ""
    )

    fun onConsentResult(success: Boolean) {
        // TODO("Not yet implemented")
    }

    fun onConnectionClick() {
        when (currentState.vpnState) {
            com.pkt.domain.dto.VpnState.DISCONNECTED -> {
                viewModelScope.launch {
                    vpnRepository.connect("929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k")
                }
            }

            com.pkt.domain.dto.VpnState.CONNECTING -> {
                viewModelScope.launch {
                    vpnRepository.disconnect()
                }
            }

            com.pkt.domain.dto.VpnState.CONNECTED -> {
                viewModelScope.launch {
                    vpnRepository.disconnect()
                }
            }
        }
    }
}
