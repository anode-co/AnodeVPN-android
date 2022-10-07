package com.pkt.domain.repository

import com.pkt.domain.dto.*

interface WalletRepository {
    suspend fun getWallets(): Result<List<Addr>>
    suspend fun getWalletName(address: String): Result<String>
    suspend fun getCurrentWallet(): Result<String>
    suspend fun isPinAvailable(address: String): Result<Boolean>
    suspend fun checkPin(address: String, pin: String): Result<Boolean>
    suspend fun checkPassword(address: String, password: String): Result<Boolean>
    suspend fun getWalletBalance(address: String): Result<Double>
    suspend fun getWalletInfo(): Result<WalletInfo>
    suspend fun getCjdnsInfo(address: String): Result<CjdnsInfo>
    suspend fun generateSeed(password: String, pin: String): Result<String>
    suspend fun createWallet(password: String, pin: String, seed: String): Result<Unit>
    suspend fun recoverWallet(password: String, seed: String): Result<Unit>
    suspend fun unlockWallet(passphrase: String, name: String?): Boolean
    suspend fun createAddress(): String
    suspend fun getWalletBalances(): Result<WalletAddressBalances>
    suspend fun getCurrentAddress(): Result<String>
    suspend fun getWalletTransactions(): Result<WalletTransactions>
}
