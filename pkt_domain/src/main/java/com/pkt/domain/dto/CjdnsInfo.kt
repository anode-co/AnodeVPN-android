package com.pkt.domain.dto

import androidx.annotation.Keep

@Keep
data class CjdnsInfo(
    val appVersion: String,
    val ipv4: String,
    val ipv6: String,
    val internetipv6: String,
    val connection: CjdnsConnection,
    val peers: List<CjdnsPeer>,
    val key: String,
    val username: String,
    val nodeUrl: String,
    val vpnExit: String,
)

@Keep
data class CjdnsConnection(
    val ip4Address: String,
    val ip6Address: String,
    val key: String,
    val ip4Alloc: Int,
    val error: String,
    val ip6Alloc: Int,
    val ip4Prefix: Int,
    val ip6Prefix: Int,
    val outgoing: Int,
)

@Keep
data class CjdnsPeer(
    val ipv4: String,
    val port: Int,
    val key: String,
    val status: String,
    val bytesIn: Long,
    val bytesOut: Long,
    val bytesLost: Long,
)
