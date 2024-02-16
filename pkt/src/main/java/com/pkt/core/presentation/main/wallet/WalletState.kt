package com.pkt.core.presentation.main.wallet

import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.main.wallet.transaction.details.TransactionDetailsExtra

data class WalletState(
    val syncState: SyncState,
    val syncTimeDiff: Long,
    val peersCount: Int,
    val chainHeight: Int,
    val walletHeight: Int,
    val neutrinoTop: Int,
    val walletName: String,
    val balancePkt: String,
    val balanceUsd: String,
    val walletAddress: String,
    val items: List<DisplayableItem>,
    val startDate: Long? = null,
    val endDate: Long? = null,
) : UiState {

    enum class SyncState {
        NOTEXISTING,
        LOCKED,
        DOWNLOADING,
        SCANNING,
        SUCCESS,
        WAITING,
        FAILED,
        NOINTERNET
    }
}

sealed class WalletEvent : UiEvent {

    object OpenSendTransaction: WalletEvent()
    object OpenVote: WalletEvent()

    data class OpenDatePicker(
        val startDate: Long?,
        val endDate: Long?,
    ) : WalletEvent()

    //object ScrollToTop: WalletEvent()

    data class OpenTransactionDetails(val extra: TransactionDetailsExtra) : WalletEvent()
}
