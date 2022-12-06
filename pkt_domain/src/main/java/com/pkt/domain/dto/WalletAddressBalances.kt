package com.pkt.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class WalletAddressBalances(
    val addrs: List<Addr>,
)

@Serializable
data class Addr(
    val address: String,
    val total: Double,
    val stotal: String,
    val spendable: Double,
    val sspendable: String,
    val immaturereward: Int,
    val simmaturereward: String,
    val unconfirmed: Int,
    val sunconfirmed: String,
    val outputcount: Int,
)