package com.pkt.domain.dto

import kotlinx.serialization.Serializable

data class Vpn(
    val name: String,
    val countryCode: String,
    val pubKey: String
)

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@Serializable
data class RequestAuthorizeVpn(
    val date: Long
)

@Serializable
data class ResponseAuthorizeVpn(
    val status: String = "",
    val message: String = ""
)

@Serializable
data class UsernameResponse(
    val username: String,
    val status: String = "",
    val message: String = ""
)