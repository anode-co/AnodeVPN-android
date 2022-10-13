package com.pkt.core.presentation.enterwallet

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class EnterWalletState(
    val wallets: Map<String, String> = emptyMap(),
    val currentWallet: String = "",
    val pin: String = "",
    val isPinAvailable: Boolean = false,
    val loginButtonEnabled: Boolean = false,
) : UiState {

    /*val currentWalletName: String
        get() = wallets[currentWallet].orEmpty()*/
    val currentWalletName: String
        get() = "wallet.db"
}

sealed class EnterWalletEvent : UiEvent {
    object ShowKeyboard : EnterWalletEvent()
    object ClearPassword : EnterWalletEvent()
    data class OpenChooseWallet(val wallets: List<String>, val currentWallet: String) : EnterWalletEvent()
}
