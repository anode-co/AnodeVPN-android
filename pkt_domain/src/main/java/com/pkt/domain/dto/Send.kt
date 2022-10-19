package com.pkt.domain.dto

data class SendResponse(
    val transactionId: String,
    val address: String,
    val blockNumber: Int,
    val amount: Double,
    val timestamp: String,
)
