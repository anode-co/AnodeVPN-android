package com.pkt.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.pkt.domain.dto.*

interface WalletRepository {
    //Return a list of all existing PKT wallets
    fun getAllWalletNames(): List<String>
    //Return the name of the currently selected wallet
    fun getActiveWallet(): String
    //Set the currently selected wallet
    suspend fun setActiveWallet(walletName: String)
    //Get the PKT wallet address that has the biggest balance of PKT
    suspend fun getWalletAddress(): Result<String>
    //Check if a stored PIN is available
    suspend fun isPinAvailable(): Result<Boolean>
    //Check if the PIN is correct
    suspend fun checkPin(pin: String): Result<Boolean>
    //Get the total available balance of PKT in all the addresses of the wallet
    suspend fun getTotalWalletBalance(): Result<Long>
    //Get the wallet balance of a specific address
    suspend fun getWalletBalance(address: String): Result<Long>
    //Get the wallet info
    suspend fun getWalletInfo(): Result<WalletInfo>
    //Generate seed words for a new PKT wallet
    suspend fun generateSeed(password: String, pin: String): Result<String>
    //Create a new PKT wallet
    suspend fun createWallet(password: String, pin: String, seed: String, walletName: String?): Result<Boolean>
    //Recover a PKT wallet
    suspend fun recoverWallet(password: String, pin:String, seed: String, seedPassword: String, walletName: String): Result<Boolean>
    //Unlock the active PKT wallet
    suspend fun unlockWallet(passphrase: String): Result<Boolean>
    //Unlock the active PKT wallet with the PIN
    suspend fun unlockWalletWithPIN(pin: String): Result<Boolean>
    //Create a new PKT address
    suspend fun createAddress(): Result<WalletAddressCreateResponse>
    //Get the list of all PKT addresses and their balances of the active wallet
    //suspend fun getWalletBalances(): Result<WalletAddressBalances>
    //Get the list of Transactions of the active wallet
    suspend fun getWalletTransactions(coinbase: Int=1, reversed: Boolean=false, skip: Int=0, limit: Int=10, startTimestamp: Long=0, endTimestamp: Long=0): Result<WalletTransactions>
    //Get the seed words of the active wallet
    suspend fun getSeed(): Result<String>
    //Rename the active wallet file in the device
    suspend fun renameWallet(name: String): Result<String?>
    //Check if wallet name exists
    suspend fun checkWalletName(name: String): Result<String?>
    //Delete wallet name from device
    suspend fun deleteWallet(name: String)
    //Change active wallet passphrase
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean>
    //Change active wallet PIN
    suspend fun changePin(password: String, pin: String)

    fun getPin(): String
    //Generate QR code for given address
    suspend fun generateQr(address: String): Result<Bitmap>
    //Check if passphrase is correct against active wallet
    suspend fun checkWalletPassphrase(passphrase: String): Result<Boolean>
    //Send PKT to an address
    suspend fun sendCoins(fromAddresses: List<String>, amount: Double, toAddress: String): Result<SendTransactionResponse>
    //Change the active wallet's passphrase
    suspend fun changePassphrase(oldPassphrase: String, newPassphrase: String): Result<Boolean>
    //Get secret
    suspend fun getSecret(): Result<String>
    //Validate PKT address
    fun isPKTAddressValid(address: String): Result<String>

    suspend fun resyncWallet()

    suspend fun getPktToUsd(): Result<Float>

    suspend fun stopPld()

    fun getActiveWalletUri(): Uri?
}
