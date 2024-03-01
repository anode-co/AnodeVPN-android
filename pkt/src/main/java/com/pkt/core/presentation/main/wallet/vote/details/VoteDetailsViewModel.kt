package com.pkt.core.presentation.main.wallet.vote.details

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class VoteDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<VoteDetailsState>() {

    private val vote: VoteDetails = savedStateHandle["vote"] ?: error("vote required")

    override fun createInitialState() = VoteDetailsState(vote = this.vote)

    fun onCopyClick() {
        sendEvent(CommonEvent.CopyToBuffer(R.string.transaction_id, vote.voteTxid))
        sendEvent(CommonEvent.Info(R.string.transaction_id_copied))
    }

    fun onSeeMoreLessClick() {
        sendState { copy(moreVisible = !moreVisible) }
    }

    fun onViewClick() {
        sendEvent(CommonEvent.WebUrl("https://explorer.pkt.cash/tx/${vote.voteTxid}"))
    }

    fun onAddressClick(address: String) {
        if (address.isNotEmpty()) {
            sendEvent(CommonEvent.CopyToBuffer(R.string.address, address))
            sendEvent(CommonEvent.Info(R.string.address_copied))
        }
    }
}
