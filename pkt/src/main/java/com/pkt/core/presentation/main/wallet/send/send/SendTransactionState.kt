package com.pkt.core.presentation.main.wallet.send.send

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class SendTransactionState(
    val maxValueSelected: Boolean = false,
    val sendButtonEnabled: Boolean = false,
) : UiState

sealed class SendTransactionEvent : UiEvent {
    object OpenKeyboard : SendTransactionEvent()
}
