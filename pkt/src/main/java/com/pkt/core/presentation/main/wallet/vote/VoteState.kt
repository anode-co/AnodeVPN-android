package com.pkt.core.presentation.main.wallet.vote

import androidx.annotation.StringRes
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.main.wallet.send.send.SendTransactionEvent

data class VoteState(
    val candidateSelected: Boolean = false,
    val withdrawVoteSelected: Boolean = false,
    val voteButtonEnabled: Boolean = false,
) : UiState

sealed class VoteEvent : UiEvent {
    object OpenKeyboard : VoteEvent()

    data class AddressError(val error: String?) : VoteEvent()

    data class OpenSendConfirm(val fromaddress: String, val toaddress: String, val amount: Double, val maxAmount: Boolean, val isVote: Boolean, val isVoteCandidate: Boolean) : VoteEvent()
}
