package com.pkt.domain.repository

import com.pkt.domain.dto.*

interface WalletRepository {
    suspend fun getWallets(): Result<List<Addr>>
    suspend fun getWalletBalance(address: String): Result<Double>
    suspend fun getWalletInfo(): Result<WalletInfo>
    suspend fun getCjdnsInfo(address: String): Result<CjdnsInfo>
    suspend fun unlockWallet(passphrase: String, name: String?): Boolean
    suspend fun createAddress(): String
    suspend fun getWalletBalances(): Result<WalletAddressBalances>
    suspend fun getCurrentAddress(): Result<String>
    suspend fun getWalletTransactions(): Result<WalletTransactions>
}
