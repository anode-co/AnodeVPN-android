package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class WalletAddressBalances(
    val addrs: List<Addr>,
)

@Keep
@Serializable
data class Addr(
    val address: String,
    val total: Double,
    val stotal: String,
    val spendable: Double,
    val sspendable: String,
    val immaturereward: Double,
    val simmaturereward: String,
    val unconfirmed: Int,
    val sunconfirmed: String,
    val outputcount: Int,
)