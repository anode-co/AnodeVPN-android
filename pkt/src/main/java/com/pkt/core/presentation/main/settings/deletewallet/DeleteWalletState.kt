package com.pkt.core.presentation.main.settings.deletewallet

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class DeleteWalletState(
    val walletName: String = "",
    val deleteButtonEnabled: Boolean = true,
) : UiState

sealed class DeleteWalletEvent : UiEvent {
    data class FillWalletName(val name: String) : DeleteWalletEvent()
    data class ShowInputError(val error: String) : DeleteWalletEvent()
    object Dismiss : DeleteWalletEvent()
}
