package com.pkt.core.presentation.main.vpn

import androidx.lifecycle.viewModelScope
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import com.pkt.core.presentation.common.state.StateViewModel
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

        invokeLoadingAction()
    }

    override fun createInitialState() = VpnState(
        ipV4 = "217.71.236.178",
        ipV6 = "2607.5300.2038.fc00.1234.2038.fc00.1234"
    )

    override fun createLoadingAction(): (suspend () -> Result<*>) = {
        vpnRepository.fetchVpnList()
    }

    fun onConsentResult(success: Boolean) {
        // TODO("Not yet implemented")
    }

    fun onConnectionClick() {
        when (currentState.vpnState) {
            com.pkt.domain.dto.VpnState.DISCONNECTED -> {
                viewModelScope.launch {
                    vpnRepository.connect()
                }
            }

            com.pkt.domain.dto.VpnState.CONNECTING -> {
                AnodeUtil.addCjdnsPeers()
                AnodeClient.AuthorizeVPN().execute()
            }

            com.pkt.domain.dto.VpnState.CONNECTED -> {
                viewModelScope.launch {
                    vpnRepository.disconnect()
                }
            }
        }
    }
}
