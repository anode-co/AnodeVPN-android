package com.pkt.domain.dto

@kotlinx.serialization.Serializable
data class CjdnsPeeringLine(
    val ip: String,
    val login: String,
    val password: String,
    val port: Int,
    val publicKey: String
)