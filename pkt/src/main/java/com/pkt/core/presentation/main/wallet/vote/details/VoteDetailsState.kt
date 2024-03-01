package com.pkt.core.presentation.main.wallet.vote.details

import com.pkt.core.presentation.common.state.UiState

data class VoteDetailsState(
    val vote: VoteDetails,
    val moreVisible: Boolean = false,
) : UiState {

    val canSeeMore: Boolean
        get() = false
}
