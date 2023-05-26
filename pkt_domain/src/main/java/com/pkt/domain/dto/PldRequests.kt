package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class GetSecretResponse(
    val secret: String,
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class GetSeedResponse(
    val seed: List<String>,
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class ChangePassphraseRequest(
    val current_passphrase: String,
    val new_passphrase: String
)

@Keep
@Serializable
data class ChangePassphraseResponse(
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class SendTransactionRequest(
    val to_address: String,
    val amount: Double,
    val from_address: List<String>
)

@Keep
@Serializable
data class CreateTransactionRequest(
    val to_address: String,
    val amount: Double,
    val from_address: List<String>,
    val sign: Boolean
)

@Keep
@Serializable
data class SendTransactionResponse(
    val txHash: String,
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class CreateTransactionResponse(
    val transaction: String,
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class CheckPassphraseRequest(
    val wallet_passphrase: String
)

@Keep
@Serializable
data class CheckPassphraseResponse(
    val validPassphrase: Boolean,
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class UnlockWalletRequest (
    var wallet_passphrase: String,
    val wallet_name: String,
)

@Keep
@Serializable
data class UnlockWalletResponse (
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class ResyncWalletResponse (
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class WalletBalancesRequest (
    var showzerobalance: Boolean,
)

@Keep
@Serializable
data class WalletTransactionsRequest (
    val coinbase: Int,
    val reversed: Boolean,
    val txnsSkip: Int,
    val txnsLimit: Int,
    val startTimestamp: Long,
    val endTimestamp: Long
)

@Keep
@Serializable
data class CreateSeedRequest (
    val seed_passphrase: String
)

@Keep
@Serializable
data class CreateSeedResponse (
    val seed: List<String>,
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class CreateWalletRequest (
    val wallet_passphrase: String,
    val seed_passphrase: String,
    val wallet_seed: List<String>,
    val wallet_name: String,
)

@Keep
@Serializable
data class RecoverWalletRequest (
    val wallet_passphrase: String,
    val wallet_seed: List<String>,
    val wallet_name: String,
)

@Keep
@Serializable
data class CreateWalletResponse (
    var message: String = "",
    val stack: String = ""
)

@Keep
@Serializable
data class WalletAddressCreateResponse (
    val address: String,
    val message: String = "",
    val stack: String = ""
)