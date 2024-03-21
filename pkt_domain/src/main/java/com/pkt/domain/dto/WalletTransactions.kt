package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class WalletTransactions(
    val transactions: List<Transaction>
)

@Keep
@Serializable
data class Transaction(
    val blockHash: String,
    val blockHeight: Int,
    val numConfirmations: Int,
    val time: String,
    val tx: Tx,
    val txBin: String
)