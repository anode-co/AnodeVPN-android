package com.pkt.core.presentation.main.wallet.send.confirm

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class SendConfirmState(
    val address: String = "",
    val amount: Double = 0.0,
) : UiState

sealed class SendConfirmEvent : UiEvent {
    object OpenKeyboard : SendConfirmEvent()
    object ClearPin : SendConfirmEvent()
}
