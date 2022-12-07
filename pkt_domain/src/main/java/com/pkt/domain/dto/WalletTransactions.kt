package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class WalletTransactions(
    val transactions: List<Transaction>
)