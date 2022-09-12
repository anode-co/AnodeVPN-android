package com.pkt.core.presentation.main.wallet

import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.UiState

data class WalletState(
    val syncState: SyncState,
    val peersCount: Int,
    val block: String,
    val walletName: String,
    val balancePkt: String,
    val balanceUsd: String,
    val walletAddress: String,
    val items: List<DisplayableItem>,
) : UiState {

    enum class SyncState {
        PROGRESS,
        SUCCESS,
        FAILED
    }
}
