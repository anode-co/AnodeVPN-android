package com.pkt.domain.interfaces

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pkt.domain.dto.VpnServer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

class VpnAPIService {
    private val apiVersion = "0.3"
    private val baseUrl = "https://vpn.anode.co/api/$apiVersion/"
    //http://v4.vpn.anode.co/api/0.3/vpn/clients/ipaddress/
    @OptIn(ExperimentalSerializationApi::class)
    private val converter = Json.asConverterFactory("application/json".toMediaType())

    private val vpnApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converter)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(VpnAPI::class.java)

    suspend fun getIPv4Address(): String {
        val api = Retrofit.Builder()
            .baseUrl("http://v4.vpn.anode.co/")
            .addConverterFactory(converter)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(VpnAPI::class.java)
        return api.getIPv4Address().ipAddress
    }

    suspend fun getIPv6Address(): String {
        val api = Retrofit.Builder()
            .baseUrl("http://v6.vpn.anode.co/")
            .addConverterFactory(converter)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(VpnAPI::class.java)
        return api.getIPv6Address().ipAddress
    }

    suspend fun getVpnServersList(): List<VpnServer> {

        return vpnApi.getVpnServersList()
    }
}