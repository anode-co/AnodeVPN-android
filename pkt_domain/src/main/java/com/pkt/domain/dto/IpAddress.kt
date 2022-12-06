package com.pkt.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class IpAddress(
    val ipAddress: String,
    val maxPrefixLength: Int,
    val version: Int
)