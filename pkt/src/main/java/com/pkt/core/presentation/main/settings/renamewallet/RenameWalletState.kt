package com.pkt.core.presentation.main.settings.renamewallet

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class RenameWalletState(
    val saveButtonEnabled: Boolean = true,
) : UiState

sealed class RenameWalletEvent : UiEvent {
    data class FillWalletName(val name: String) : RenameWalletEvent()
    data class ShowInputError(val error: String) : RenameWalletEvent()
    object Dismiss : RenameWalletEvent()
}
