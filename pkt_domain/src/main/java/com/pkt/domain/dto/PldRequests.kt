package com.pkt.domain.dto

import kotlinx.serialization.Serializable
import org.json.JSONArray

@Serializable
data class UnlockWalletRequest (
    var wallet_passphrase: String,
    val wallet_name: String,
)

@Serializable
data class UnlockWalletResponse (
    var message: String = "",
    val stack: String = ""
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

@Serializable
data class CreateSeedRequest (
    val seed_passphrase: String
)

@Serializable
data class CreateSeedResponse (
    val seed: List<String>,
    var message: String = "",
    val stack: String = ""
)

@Serializable
data class CreateWalletRequest (
    val wallet_passphrase: String,
    val seed_passphrase: String,
    val wallet_seed: List<String>,
    val wallet_name: String,
)

@Serializable
data class RecoverWalletRequest (
    val wallet_passphrase: String,
    val wallet_seed: List<String>,
    val wallet_name: String,
)

@Serializable
data class CreateWalletResponse (
    var message: String = "",
    val stack: String = ""
)