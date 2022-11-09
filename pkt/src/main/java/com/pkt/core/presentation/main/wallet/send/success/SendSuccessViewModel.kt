package com.pkt.core.presentation.main.wallet.send.success

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class SendSuccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<SendSuccessState>() {

    private val transactionId: String = savedStateHandle["transactionId"] ?: error("transactionId required")

    override fun createInitialState() = SendSuccessState(transactionId = this.transactionId)

    fun onCopyClick() {
        sendEvent(CommonEvent.CopyToBuffer(R.string.transaction_id, transactionId))
        sendEvent(CommonEvent.Info(R.string.transaction_id_copied))
    }

    fun onViewClick() {
        sendEvent(CommonEvent.WebUrl("https://explorer.pkt.cash/tx/$transactionId"))
    }
}
