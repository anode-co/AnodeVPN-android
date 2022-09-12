package com.pkt.dummy.dto

import kotlinx.serialization.Serializable

@Serializable
data class CjdnsInfoDummy(
    val appVersion: String,
    val ipv4: String,
    val ipv6: String,
    val internetipv6: String,
    val connection: CjdnsConnectionDummy,
    val peers: List<CjdnsPeerDummy>,
    val key: String,
    val username: String,
    val nodeUrl: String,
    val vpnExit: String,
)

@Serializable
data class CjdnsConnectionDummy(
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

@Serializable
data class CjdnsPeerDummy(
    val ipv4: String,
    val port: Int,
    val key: String,
    val status: String,
    val bytesIn: Long,
    val bytesOut: Long,
    val bytesLost: Long,
)
