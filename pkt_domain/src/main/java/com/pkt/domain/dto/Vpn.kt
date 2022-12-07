package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
data class Vpn(
    val name: String,
    val countryCode: String,
    val publicKey: String
)

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    GETTING_ROUTES,
    GOT_ROUTES,
    CONNECTED,
    NO_INTERNET
}

@Keep
@Serializable
data class RequestAuthorizeVpn(
    val date: Long
)

@Keep
@Serializable
data class ResponseAuthorizeVpn(
    val status: String = "",
    val message: String = ""
)

@Keep
@Serializable
data class UsernameResponse(
    val username: String,
    val status: String = "",
    val message: String = ""
)