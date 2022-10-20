package com.pkt.domain.interfaces

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pkt.domain.dto.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.Body
import timber.log.Timber
import java.lang.reflect.Type


class WalletAPIService {

    private val baseUrl = "http://localhost:8080/api/v1/"
    private val converter = Json.asConverterFactory("application/json".toMediaType())
    //@OptIn(ExperimentalSerializationApi::class)
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converter)
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(WalletAPI::class.java)

    suspend fun getWalletInfo(): WalletInfo {
        return api.getWalletInfo()
    }

    suspend fun unlockWalletAPI(@Body unlockWalletRequest: UnlockWalletRequest): Boolean {
        val response = api.unlockWallet(unlockWalletRequest)
        if (response.body()?.message == "") {
            return true
        } else {
            Timber.d("unlockWalletAPI: failed with message ${response.errorBody()?.string()}")
            return false
        }
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

class NullOnEmptyConverterFactory : Converter.Factory() {
    fun responseBody(type: Type?, annotations: Array<Annotation?>?, retrofit: Retrofit): Converter<ResponseBody?, *>? {
        val delegate: Converter<ResponseBody, String> = retrofit.nextResponseBodyConverter(this, type, annotations)
        return Converter { body ->
            if (body?.contentLength() == 0L)
                null
            else
                delegate.convert(body)
        }
    }
}
