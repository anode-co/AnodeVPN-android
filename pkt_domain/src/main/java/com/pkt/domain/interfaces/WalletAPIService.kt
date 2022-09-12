package com.pkt.domain.interfaces

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pkt.domain.dto.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.Body

class WalletAPIService {

    private val baseUrl = "http://localhost:8080/api/v1/"
    private val converter = Json.asConverterFactory("application/json".toMediaType())
    //@OptIn(ExperimentalSerializationApi::class)
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converter)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(WalletAPI::class.java)

    suspend fun getWalletInfo(): WalletInfo {
        return api.getWalletInfo()
    }

    suspend fun unlockWalletAPI(@Body unlockWalletRequest: UnlockWalletRequest) {
        return api.unlockWallet(unlockWalletRequest)
    }

    suspend fun createAddress(): String {
        return api.createAddress()
    }

    suspend fun getWalletBalances(showzerobalances: Boolean): WalletAddressBalances {
        val request: WalletBalancesRequest = WalletBalancesRequest(showzerobalances)
        return api.getWalletBalances(request)
    }

    suspend fun getWalletTransactions(coinbase: Int, reversed: Boolean, txnsSkip: Int, txnsLimit: Int) : WalletTransactions {
        val request = WalletTransactionsRequest(coinbase, reversed, txnsSkip, txnsLimit)
        return api.getWalletTransactions(request)
    }
}