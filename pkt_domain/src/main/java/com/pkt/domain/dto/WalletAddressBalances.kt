package com.pkt.domain.dto

data class WalletAddressBalances(
    val addrs: List<Addr>,
)

data class Addr(
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
