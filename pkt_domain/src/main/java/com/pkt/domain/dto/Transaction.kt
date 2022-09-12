package com.pkt.domain.dto

import kotlinx.serialization.Serializable

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