package com.pkt.domain.interfaces

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pkt.domain.dto.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import timber.log.Timber
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


class WalletAPIService {

    private val baseUrl = "http://localhost:8080/api/v1/"
    @OptIn(ExperimentalSerializationApi::class)
    private val converter = Json.asConverterFactory("application/json".toMediaType())
    //OkhttpClient to introduce timeout
    //private val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.HOURS)
        .writeTimeout(30, TimeUnit.MINUTES)
        //.addInterceptor(interceptor)
        .build()
    private val api = Retrofit.Builder()
        .client(httpClient)
        .baseUrl(baseUrl)
        .addConverterFactory(converter)
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(WalletAPI::class.java)

    suspend fun getWalletInfo(): WalletInfo {
        return api.getWalletInfo()
    }

    suspend fun unlockWalletAPI(@Body unlockWalletRequest: UnlockWalletRequest): Boolean {
        try {
            val response = api.unlockWallet(unlockWalletRequest)
            if (response.body()?.message == "") {
                return true
            } else {
                Timber.d("unlockWalletAPI: failed with message ${response.errorBody()?.string()}")
                return false
            }
        }catch (e: SocketTimeoutException){
            Timber.d("unlockWalletAPI: timed out")
            return false
        }catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

    suspend fun createAddress(): WalletAddressCreateResponse {
        try {
            return api.createAddress("{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
        } catch (e: Exception) {
            Timber.d("unlockWalletAPI: failed with message ${e.message}")
            return WalletAddressCreateResponse("","${e.message}", e.stackTraceToString())
        }
    }


    suspend fun getWalletBalances(showzerobalances: Boolean): WalletAddressBalances {
        val request = WalletBalancesRequest(showzerobalances)
        return api.getWalletBalances(request)
    }

    suspend fun getWalletTransactions(coinbase: Int, reversed: Boolean, txnsSkip: Int, txnsLimit: Int, startTimestamp: Long, endTimestamp: Long) : WalletTransactions {
        val request = WalletTransactionsRequest(coinbase, reversed, txnsSkip, txnsLimit, startTimestamp, endTimestamp)
        return api.getWalletTransactions(request)
    }

    suspend fun createSeed(seed_passphrase: String): CreateSeedResponse {
        val request = CreateSeedRequest(seed_passphrase)
        try {
            return api.createSeed(request)
        } catch (e: Exception) {
            Timber.d("createSeed: failed with message ${e.message}")
            return CreateSeedResponse(listOf(), "${e.message}", e.stackTraceToString())
        }
    }

    suspend fun createWallet(wallet_passphrase: String, seed_passphrase: String, wallet_seed: List<String>, wallet_name: String): CreateWalletResponse {
        val request = CreateWalletRequest(wallet_passphrase, seed_passphrase, wallet_seed, wallet_name)
        try {
            return api.createWallet(request)
        } catch (e: Exception) {
            Timber.d("createWallet: failed with message ${e.message}")
            return CreateWalletResponse("${e.message}", e.stackTraceToString())
        }
    }

    suspend fun recoverWallet(wallet_passphrase: String, seed_passphrase: String, wallet_seed: List<String>, wallet_name: String): CreateWalletResponse {
        if (seed_passphrase.isNotEmpty()) {
            val request = CreateWalletRequest(wallet_passphrase, seed_passphrase, wallet_seed, wallet_name)
            return api.createWallet(request)
        } else {
            //with empty seed passphrase
            val request = RecoverWalletRequest(wallet_passphrase, wallet_seed, wallet_name)
            return api.createWallet(request)
        }
    }

    suspend fun sendTransaction(request: SendTransactionRequest): SendTransactionResponse {
        try {
            return api.sendTransaction(request)
        } catch (e: Exception) {
            Timber.d("sendTransaction: failed with message ${e.message}")
            return SendTransactionResponse("","${e.message}", e.stackTraceToString())
        }
    }

    suspend fun checkPassphrase(request: CheckPassphraseRequest): CheckPassphraseResponse {
        return api.checkPassphrase(request)
    }

    suspend fun changePassphrase(request: ChangePassphraseRequest): ChangePassphraseResponse {
        return api.changePassphrase(request)
    }

    suspend fun getWalletSeed(): GetSeedResponse {
        return api.getWalletSeed()
    }

    suspend fun getSecret(): GetSecretResponse {
        return api.getSecret("{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
    }

    suspend fun getPktToUsd(): Float {
        val priceApi = Retrofit.Builder()
            .baseUrl("https://pkt.cash/")
            .addConverterFactory(converter)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(WalletAPI::class.java)

        try {
            val response = priceApi.getPktToUSD()
            return response.data.PKT.quote.USD.price.toFloat()
        }catch (e: Exception) {
            Timber.d("getPktToUsd: failed with message ${e.message}")
            return 0.0f
        }
    }

    suspend fun resyncWallet() {
        val response = api.resyncWallet("{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
        if (response.message == "") {
            Timber.d("resyncWallet: success")
        } else {
            Timber.d("resyncWallet: failed with message ${response.message}")
        }
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

class EmptyRequest {
    val INSTANCE: EmptyRequest = EmptyRequest()
}

