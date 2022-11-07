package com.pkt.domain.interfaces

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pkt.domain.dto.CjdnsPeeringLine
import com.pkt.domain.dto.RequestAuthorizeVpn
import com.pkt.domain.dto.VpnServer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.*

class VpnAPIService {
    private val apiVersion = "0.3"
    private val baseUrl = "https://vpn.anode.co/api/$apiVersion/"

    @OptIn(ExperimentalSerializationApi::class)
    private val converter = Json.asConverterFactory("application/json".toMediaType())

    private val vpnApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(VpnAPI::class.java)

    suspend fun getIPv4Address(): String {
        val api = Retrofit.Builder()
            .baseUrl("http://v4.vpn.anode.co/api/$apiVersion/")
            .addConverterFactory(converter)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(VpnAPI::class.java)
        return api.getIPv4Address().ipAddress
    }

    suspend fun getIPv6Address(): String {
        val api = Retrofit.Builder()
            .baseUrl("http://v6.vpn.anode.co/api/$apiVersion/")
            .addConverterFactory(converter)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(VpnAPI::class.java)
        return api.getIPv6Address().ipAddress
    }

    suspend fun getVpnServersList():List<VpnServer> {
        return vpnApi.getVpnServersList()
    }

    suspend fun authorizeVPN(signature: String, pubKey:String, date: Long):Result<Boolean> {
        val url = baseUrl+"vpn/servers/$pubKey/"
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(httpClient)
            .build()
            .create(VpnAPI::class.java)
        val request = RequestAuthorizeVpn(date)
        val response = api.authorizeVPN("cjdns $signature", request)
        if (response.status == "success") {
            return Result.success(true)
        } else {
            return Result.failure(Exception(response.message))
        }
    }

    suspend fun getCjdnsPeeringLines(): List<CjdnsPeeringLine> {
        return vpnApi.getCjdnsPeeringLines()
    }
}