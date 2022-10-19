package com.pkt.core.presentation.main.vpn

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState
import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnState

data class VpnState(
    val vpn: Vpn? = null,
    val vpnState: VpnState = VpnState.DISCONNECTED,
    val ipV4: String? = null,
    val ipV6: String? = null,
) : UiState

sealed class VpnEvent : UiEvent {
    object OpenConsent : VpnEvent()
}
