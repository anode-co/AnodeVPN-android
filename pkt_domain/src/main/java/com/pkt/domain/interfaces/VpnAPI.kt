package com.pkt.domain.interfaces

import com.pkt.domain.dto.*
import retrofit2.http.*

interface VpnAPI {
    @GET("vpn/servers/")
    suspend fun getVpnServersList(): List<VpnServer>
    @GET("vpn/clients/ipaddress/")
    suspend fun getIPv4Address(): IpAddress
    @GET("vpn/clients/ipaddress/")
    suspend fun getIPv6Address(): IpAddress
    @GET("vpn/cjdns/peeringlines/")
    suspend fun getCjdnsPeeringLines(): List<CjdnsPeeringLine>
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("authorize/")
    suspend fun authorizeVPN(@Header("Authorization") signature: String, @Body requestAuthorizeVpn:RequestAuthorizeVpn): ResponseAuthorizeVpn
    @POST("vpn/clients/events/")
    fun postError(@Body error: String): ResponseErrorPost
    @Headers("Content-Type: application/json; charset=utf-8")
    @GET("vpn/accounts/username/")
    suspend fun generateUsername(@Header("Authorization") signature: String): UsernameResponse
}