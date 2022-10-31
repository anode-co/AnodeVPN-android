package com.pkt.core.presentation.main.settings.newwallet

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class NewWalletState(
    val nextButtonEnabled: Boolean = true,
) : UiState

sealed class NewWalletEvent : UiEvent {
    data class ShowInputError(val error: String) : NewWalletEvent()
    data class Success(val name: String) : NewWalletEvent()
}
