package com.pkt.core.presentation.main.vpn.exits

import com.pkt.core.presentation.common.state.UiState

data class VpnExitsState(
    val items: List<VpnExitItem> = emptyList(),
) : UiState
