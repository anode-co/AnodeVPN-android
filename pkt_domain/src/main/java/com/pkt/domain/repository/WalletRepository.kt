package com.pkt.domain.repository

import com.pkt.domain.dto.*

interface WalletRepository {
    suspend fun getAllWalletNames(): Result<List<String>>
    suspend fun getActiveWallet(): Result<String>
    suspend fun setActiveWallet(walletName: String)
    suspend fun getWalletAddress(): Result<String>
    suspend fun isPinAvailable(): Result<Boolean>
    suspend fun checkPin(pin: String): Result<Boolean>
    suspend fun getTotalWalletBalance(): Result<Double>
    suspend fun getWalletBalance(address: String): Result<Double>
    suspend fun getWalletInfo(): Result<WalletInfo>
    suspend fun getCjdnsInfo(address: String): Result<CjdnsInfo>
    suspend fun generateSeed(password: String, pin: String): Result<String>
    suspend fun createWallet(password: String, pin: String, seed: String, walletName: String): Result<Boolean>
    suspend fun recoverWallet(password: String, seed: String, seed_password: String, walletName: String): Result<Boolean>
    suspend fun unlockWallet(passphrase: String): Result<Boolean>
    suspend fun unlockWalletWithPIN(pin: String): Result<Boolean>
    suspend fun createAddress(): String
    suspend fun getWalletBalances(): Result<WalletAddressBalances>
    suspend fun getCurrentAddress(): Result<String>
    suspend fun getWalletTransactions(): Result<WalletTransactions>
    suspend fun send(address: String, amount: Double): Result<SendResponse>
    suspend fun getSeed(): Result<String>
    suspend fun renameWallet(name: String): Result<String?>
}
