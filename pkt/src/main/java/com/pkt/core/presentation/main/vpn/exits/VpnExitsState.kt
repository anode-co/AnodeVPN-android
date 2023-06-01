package com.pkt.core.presentation.main.vpn.exits

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.main.settings.SettingsEvent

data class VpnExitsState(
    val items: List<VpnExitItem> = emptyList(),
) : UiState

sealed class VpnExitsEvent : UiEvent {

    data class OpenVpnSelection(val cost: Int): VpnExitsEvent()
}