package com.pkt.core.presentation.main.wallet.transaction.details

import com.pkt.core.presentation.common.state.UiState

data class TransactionDetailsState(
    val extra: TransactionDetailsExtra,
    val moreVisible: Boolean = false,
) : UiState {

    val canSeeMore: Boolean
        get() = extra.addresses.size > 1
}
