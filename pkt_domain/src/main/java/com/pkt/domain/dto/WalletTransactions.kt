package com.pkt.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class WalletTransactions(
    val transactions: List<Transaction>
)