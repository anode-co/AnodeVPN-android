package com.pkt.core.presentation.main.wallet.send.success

import com.pkt.core.presentation.common.state.UiState

data class SendSuccessState(
    val transactionId: String = "",
    val address: String = "",
) : UiState
