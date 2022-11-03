package com.pkt.core.presentation.main.wallet

import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.UiState

data class WalletState(
    val syncState: SyncState,
    val syncTimeDiff: Long,
    val peersCount: Int,
    val block: String,
    val chainHeight: Int,
    val walletHeight: Int,
    val neutrinoTop: Int,
    val walletName: String,
    val balancePkt: String,
    val balanceUsd: String,
    val walletAddress: String,
    val items: List<DisplayableItem>,
) : UiState {

    enum class SyncState {
        DOWNLOADING,
        SCANNING,
        SUCCESS,
        WAITING,
        FAILED
    }
}
