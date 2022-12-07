package com.pkt.domain.dto

import androidx.annotation.Keep

@Keep
data class SendResponse(
    val transactionId: String,
    val address: String,
    val blockNumber: Int,
    val amount: Double,
    val timestamp: String,
)
