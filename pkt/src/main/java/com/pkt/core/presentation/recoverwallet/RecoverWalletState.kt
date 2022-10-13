package com.pkt.core.presentation.recoverwallet

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class RecoverWalletState(
    val nextButtonEnabled: Boolean = false,
) : UiState

sealed class RecoverWalletEvent : UiEvent {

    data class SeedError(val error: String) : RecoverWalletEvent()
    data class PasswordError(val error: String) : RecoverWalletEvent()
}
