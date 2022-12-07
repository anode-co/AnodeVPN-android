package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class IpAddress(
    val ipAddress: String,
    val maxPrefixLength: Int,
    val version: Int
)