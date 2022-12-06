package com.pkt.core.presentation.start

import com.pkt.core.presentation.common.state.UiState

data class StartState(
    val contentVisible: Boolean = false,
) : UiState
