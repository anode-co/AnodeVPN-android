package com.pkt.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class UnlockWalletRequest (
    var wallet_passphrase: String,
    val wallet_name: String,
)

@Serializable
data class WalletBalancesRequest (
    var showzerobalance: Boolean,
)

@Serializable
data class WalletTransactionsRequest (
    val coinbase: Int,
    val reversed: Boolean,
    val txnsSkip: Int,
    val txnsLimit: Int,
)