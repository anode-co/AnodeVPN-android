package com.pkt.domain.dto

data class IpAddress(
    val ipAddress: String,
    val maxPrefixLength: Int,
    val version: Int
)