package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Keep
@Serializable
data class PktPrice(
    val `data`: Data,
    @Transient val status: Status = Status(0, 0, 0, "", "", "")
)

@Keep
@Serializable
data class Data(
    val PKT: PKT
)

@Keep
@Serializable
data class PKT(
    @Transient val circulating_supply: Double = 0.0,
    @Transient val cmc_rank: Int = 0,
    @Transient val date_added: String = "",
    @Transient val id: Int = 0,
    @Transient val is_active: Int = 0,
    @Transient val is_fiat: Int = 0,
    @Transient val last_updated: String = "",
    @Transient val max_supply: Long  = 0,
    @Transient val name: String = "",
    @Transient val num_market_pairs: Int = 0,
    @Transient val platform: String = "",
    val quote: Quote,
    @Transient val self_reported_circulating_supply: Long = 0,
    @Transient val self_reported_market_cap: Long = 0,
    @Transient val slug: String = "",
    @Transient val symbol: String = "",
    @Transient val tags: List<String> = emptyList(),
    @Transient val total_supply: Long = 0,
    @Transient val tvl_ratio: Long = 0,
    @Transient val infinite_supply: Boolean = false
)

@Keep
@Serializable
data class Quote(
    val USD: USD
)

@Keep
@Serializable
data class USD(
    @Transient val fully_diluted_market_cap: Double = 0.0,
    @Transient val last_updated: String = "",
    @Transient val market_cap: Double = 0.0,
    @Transient val market_cap_dominance: Int = 0,
    @Transient val percent_change_1h: Double = 0.0,
    @Transient val percent_change_24h: Double = 0.0,
    @Transient val percent_change_30d: Double = 0.0,
    @Transient val percent_change_60d: Double = 0.0,
    @Transient val percent_change_7d: Double = 0.0,
    @Transient val percent_change_90d: Double = 0.0,
    val price: Double,
    @Transient val tvl: Long = 0,
    @Transient val volume_24h: Double = 0.0,
    @Transient val volume_change_24h: Double = 0.0
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