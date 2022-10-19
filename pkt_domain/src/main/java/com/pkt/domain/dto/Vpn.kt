package com.pkt.domain.dto

data class Vpn(
    val name: String,
    val countryCode: String,
)

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
