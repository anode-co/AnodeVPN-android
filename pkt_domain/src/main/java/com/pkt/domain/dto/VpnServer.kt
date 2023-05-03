package com.pkt.domain.dto
import androidx.annotation.Keep
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class VpnServer(
    val average_rating: String,
    val cost: String,
    val country_code: String,
    //val created_at: LocalDateTime,
    val is_favorite: Boolean,
    //val last_seen_at: LocalDateTime,
    //val last_seen_datetime: LocalDateTime,
    val load: Int,
    val name: String,
    val num_ratings: Int,
    //val online_since_datetime: LocalDateTime,
    val public_key: String,
    val quality: Int,
    val is_active: Boolean,
)