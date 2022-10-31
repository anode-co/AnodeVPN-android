package com.pkt.domain.interfaces

import com.pkt.domain.dto.WalletInfo
import com.pkt.domain.dto.*
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WalletAPI {
    @GET("meta/getinfo")
    suspend fun getWalletInfo(): WalletInfo
    @POST("wallet/unlock")
    suspend fun unlockWallet(@Body unlockWalletRequest: UnlockWalletRequest): Response<UnlockWalletResponse>
    @POST("wallet/address/create")
    suspend fun createAddress(@Body empty: RequestBody): WalletAddressCreateResponse
    @POST("wallet/address/balances")
    suspend fun getWalletBalances(@Body walletBalancesRequest: WalletBalancesRequest): WalletAddressBalances
    @POST("wallet/transaction/query")
    suspend fun getWalletTransactions(@Body walletTransactionsRequest: WalletTransactionsRequest): WalletTransactions
    @POST("util/seed/create")
    suspend fun createSeed(@Body createSeedRequest: CreateSeedRequest): CreateSeedResponse
    @POST("wallet/create")
    suspend fun createWallet(@Body createWalletRequest: CreateWalletRequest): CreateWalletResponse
    @POST("wallet/create")
    suspend fun createWallet(@Body recoverWalletRequest: RecoverWalletRequest): CreateWalletResponse
    //wallet/getsecret
    //wallet/checkpassphrase
    //wallet/changepassphrase
    //wallet/transaction/sendfrom
    //wallet/seed
}