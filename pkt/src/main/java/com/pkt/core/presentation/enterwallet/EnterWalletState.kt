package com.pkt.core.presentation.enterwallet

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class EnterWalletState(
    val wallets: List<String> = emptyList(),
    val currentWallet: String = "",
    val pin: String = "",
    val isPinVisible: Boolean = false,
    val loginButtonEnabled: Boolean = false,
    val enterPasswordButtonVisible: Boolean = false,
    val enterPinButtonVisible: Boolean = false,
) : UiState

sealed class EnterWalletEvent : UiEvent {
    object ShowKeyboard : EnterWalletEvent()
    object ClearPassword : EnterWalletEvent()
    data class OpenChooseWallet(val wallets: List<String>, val currentWallet: String) : EnterWalletEvent()
}
