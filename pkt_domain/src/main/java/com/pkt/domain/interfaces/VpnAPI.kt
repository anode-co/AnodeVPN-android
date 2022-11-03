package com.pkt.domain.interfaces

import com.pkt.domain.dto.*
import retrofit2.http.*

interface VpnAPI {
    @GET("vpn/servers/")
    suspend fun getVpnServersList(): List<VpnServer>
    @GET("api/0.3/vpn/clients/ipaddress/")
    suspend fun getIPv4Address(): IpAddress
    @GET("api/0.3/vpn/clients/ipaddress/")
    suspend fun getIPv6Address(): IpAddress
    //GenerateUsername
    //PostError
    //AuthorizeVPN
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("authorize/")
    suspend fun authorizeVPN(@Header("Authorization") signature: String, @Body requestAuthorizeVpn:RequestAuthorizeVpn): ResponseAuthorizeVpn
}