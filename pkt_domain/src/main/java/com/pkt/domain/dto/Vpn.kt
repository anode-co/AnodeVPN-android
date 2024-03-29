package com.pkt.domain.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
data class Vpn(
    val name: String,
    val countryCode: String = "CA",
    val publicKey: String,
    val isActive: Boolean,
    var isPremium: Boolean = false,
    var requestPremium: Boolean = false,
    val cost: Int = 0,
)

enum class VpnState {
    DISCONNECTED,
    CONNECT,
    CONNECTING,
    GETTING_ROUTES,
    GOT_ROUTES,
    CONNECTED,
    CONNECTED_PREMIUM,
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