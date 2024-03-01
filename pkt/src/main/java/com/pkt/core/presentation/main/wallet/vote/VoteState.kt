package com.pkt.core.presentation.main.wallet.vote

import androidx.annotation.StringRes
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class VoteState(
    val candidateSelected: Boolean = false,
    val withdrawVoteSelected: Boolean = false,
    val voteButtonEnabled: Boolean = false,
) : UiState

sealed class VoteEvent : UiEvent {
    object OpenKeyboard : VoteEvent()

    data class AddressError(val error: String?) : VoteEvent()
}
