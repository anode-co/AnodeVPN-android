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
    //[{"ip":"51.222.109.178","port":32767,"login":"anode.co/cjdns","password":"m1g8rg77cj86qdt7471wpcdpg3c6j7x","publicKey":"929cwrjn11muk4cs5pwkdc5f56hu475wrlhq90pb9g38pp447640.k"},{"ip":"207.246.116.12","port":59413,"login":"anode.co/cjdns","password":"f4xmmsqvhz72pl7zf7jqwjffc644g14","publicKey":"1y7k7zb64f242hvv8mht54ssvgcqdfzbxrng5uz7qpgu7fkjudd0.k"}]
    val status: String = "",
    val message: String = ""
)
