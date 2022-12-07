package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Transaction(
    val amount: String,
    val blockHash: String,
    val blockHeight: Int,
    val destAddresses: List<String>,
    val label: String,
    val numConfirmations: Int,
    val rawTxHex: String,
    val timeStamp: String,
    val totalFees: String,
    val txHash: String
)