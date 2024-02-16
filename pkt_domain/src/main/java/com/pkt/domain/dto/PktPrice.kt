package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class PktPrice(
    val `data`: Data,
    val status: Status
)

@Keep
@Serializable
data class Data(
    val PKT: PKT
)

@Keep
@Serializable
data class PKT(
    val circulating_supply: Double,
    val cmc_rank: Int,
    val date_added: String,
    val id: Int,
    val is_active: Int,
    val is_fiat: Int,
    val last_updated: String,
    val max_supply: Long,
    val name: String,
    val num_market_pairs: Int,
    val platform: String?,
    val quote: Quote,
    val self_reported_circulating_supply: Long?,
    val self_reported_market_cap: Long?,
    val slug: String,
    val symbol: String,
    val tags: List<String>,
    val total_supply: Long,
    val tvl_ratio: Long?,
    val infinite_supply: Boolean
)

@Keep
@Serializable
data class Quote(
    val USD: USD
)

@Keep
@Serializable
data class USD(
    val fully_diluted_market_cap: Double,
    val last_updated: String,
    val market_cap: Double,
    val market_cap_dominance: Int,
    val percent_change_1h: Double,
    val percent_change_24h: Double,
    val percent_change_30d: Double,
    val percent_change_60d: Double,
    val percent_change_7d: Double,
    val percent_change_90d: Double,
    val price: Double,
    val tvl: Long?,
    val volume_24h: Double,
    val volume_change_24h: Double
)

@Keep
@Serializable
data class Status(
    val credit_count: Int,
    val elapsed: Int,
    val error_code: Int,
    val error_message: String?,
    val notice: String?,
    val timestamp: String
)