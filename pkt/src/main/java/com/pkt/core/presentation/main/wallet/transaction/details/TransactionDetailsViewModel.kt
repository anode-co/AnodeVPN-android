package com.pkt.core.presentation.main.wallet.transaction.details

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<TransactionDetailsState>() {

    private val extra: TransactionDetailsExtra = savedStateHandle["extra"] ?: error("extra required")

    override fun createInitialState() = TransactionDetailsState(extra = this.extra)

    fun onCopyClick() {
        sendEvent(CommonEvent.CopyToBuffer(R.string.transaction_id, extra.transactionId))
        sendEvent(CommonEvent.Info(R.string.transaction_id_copied))
    }

    fun onSeeMoreLessClick() {
        sendState { copy(moreVisible = !moreVisible) }
    }
}
