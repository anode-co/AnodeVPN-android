package com.pkt.dummy.dto

import kotlinx.serialization.Serializable

@Serializable
data class WalletAddressBalancesDummy(
    val addrs: List<AddrDummy>,
)

@Serializable
data class AddrDummy(
    val address: String,
    val immaturereward: Int,
    val outputcount: Int,
    val simmaturereward: String,
    val spendable: Double,
    val sspendable: String,
    val stotal: String,
    val sunconfirmed: String,
    val total: Double,
    val unconfirmed: Int,
)
