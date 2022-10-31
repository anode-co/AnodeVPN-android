package com.pkt.domain.dto
import kotlinx.serialization.Serializable

@Serializable
data class VpnServer(
    val averageRating: String,
    val cost: String,
    val countryCode: String,
    val createdAt: Int,
    val isFavorite: Boolean,
    val lastSeenAt: Int,
    val lastSeenDatetime: String,
    val load: Int,
    val name: String,
    val numRatings: Int,
    val onlineSinceDatetime: String,
    val publicKey: String,
    val quality: Int
)