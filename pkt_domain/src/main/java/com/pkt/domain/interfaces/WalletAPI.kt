package com.pkt.domain.interfaces

import com.pkt.domain.dto.WalletInfo
import com.pkt.domain.dto.*
import okhttp3.RequestBody
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
    @POST("wallet/transaction/sendfrom")
    suspend fun sendTransaction(@Body sendRequest: SendTransactionRequest): SendTransactionResponse
    @POST("wallet/transaction/create")
    suspend fun createTransaction(@Body createRequest: CreateTransactionRequest): CreateTransactionResponse
    @POST("wallet/checkpassphrase")
    suspend fun checkPassphrase(@Body checkPassphraseRequest: CheckPassphraseRequest): CheckPassphraseResponse
    @POST("wallet/changepassphrase")
    suspend fun changePassphrase(@Body changePassphraseRequest: ChangePassphraseRequest): ChangePassphraseResponse
    @GET("wallet/seed")
    suspend fun getWalletSeed(): GetSeedResponse
    @POST
    suspend fun getSecret(@Body empty: RequestBody): GetSecretResponse
    @GET("api/cmc-price")
    suspend fun getPktToUSD(): PktPrice
    @POST
    suspend fun resyncWallet(@Body empty: RequestBody): ResyncWalletResponse
    @POST("util/transaction/decode")
    suspend fun decodeTransaction(@Body decodeTransactionRequest: DecodeTransactionRequest): DecodeTransactionResponse
}