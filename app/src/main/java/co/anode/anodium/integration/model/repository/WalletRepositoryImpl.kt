package co.anode.anodium.integration.model.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import co.anode.anodium.support.AnodeUtil
import com.pkt.domain.dto.*
import com.pkt.domain.interfaces.WalletAPIService
import com.pkt.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor() : WalletRepository {
    private val walletAPI = WalletAPIService()
    private var activeWallet = AnodeUtil.DEFAULT_WALLET_NAME

    override fun getAllWalletNames(): List<String> {
        return AnodeUtil.getWalletFiles()
    }

    override fun getActiveWallet(): String {
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("activeWallet", "wallet")?.let {
            activeWallet = it
        }
        return activeWallet
    }

    override suspend fun setActiveWallet(walletName: String) {
        //delete existing chain back up file when changing active wallet
        if (walletName != activeWallet) {
            AnodeUtil.deleteWalletChainBackupFile()
        }
        activeWallet = walletName
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putString("activeWallet", walletName)?.apply()
    }

    override suspend fun isPinAvailable(): Result<Boolean> {
        return if (AnodeUtil.getWalletPin(activeWallet).isNotEmpty()) {
            Result.success(true)
        } else {
            Result.success(false)
        }
    }

    override suspend fun checkPin(pin: String): Result<Boolean> {
        val storedPin = AnodeUtil.getWalletPin(activeWallet)
        return Result.success(pin == storedPin)
    }

    //Get the wallet balance from all available addresses
    override suspend fun getTotalWalletBalance(): Result<Long> {
        val addresses = walletAPI.getWalletBalances(true)
        //Return the balance of address
        var balance:Long = 0
        for (i in 0 until addresses.addrs.size) {
            balance = addresses.addrs[i].stotal.toLong()
        }
        return Result.success(balance)
    }

    //Get wallet balance, if address is empty then return total balance of all addresses
    override suspend fun getWalletBalance(address: String): Result<Long> =
        runCatching {
            val addresses = walletAPI.getWalletBalances(true)
            //Return the balance of address
            var balance: Long = 0
            for (i in 0 until addresses.addrs.size) {
                if (address.isEmpty()) {
                    balance += addresses.addrs[i].stotal.toLong()
                } else if (addresses.addrs[i].address == address) {
                    balance = addresses.addrs[i].stotal.toLong()
                }
            }
            balance
        }.onSuccess {
            Timber.d("getWalletBalance: success")
        }.onFailure {
            Timber.d("getWalletBalance: success")
        }

    override suspend fun getWalletInfo(): Result<WalletInfo> = withContext(Dispatchers.IO){
        runCatching {
            walletAPI.getWalletInfo()
        }.onSuccess {
            Timber.d("getWalletInfo: success")
        }.onFailure {
            Timber.e(it, "getWalletInfo: failure")
        }
    }

    override suspend fun generateSeed(password: String, pin: String): Result<String> {
        val response = walletAPI.createSeed(password)

        if (response.seed.isNotEmpty()) {
            return Result.success(response.seed.joinToString(" "))
        } else {
            return Result.failure(Exception("Failed to generate seed: ${response.message}"))
        }
    }

    override suspend fun getWalletAddress(): Result<String> =
        runCatching {
            val addresses = walletAPI.getWalletBalances(true)
            //Return the balance of address
            var address = ""
            if (addresses != null) {
                if (addresses.addrs.size == 1) {
                    address = addresses.addrs[0].address
                } else if (addresses.addrs.size > 1) {
                    //Find address with the biggest balance
                    var biggestBalance = 0.0
                    for (i in 0 until addresses.addrs.size) {
                        val balance = addresses.addrs[i].total
                        if (balance > biggestBalance) {
                            biggestBalance = balance
                            address = addresses.addrs[i].address
                        }
                    }
                }
            }
            address
        }.onSuccess { address ->
            Timber.d("getWalletAddress: success: $address")
            address
        }.onFailure {
            Timber.e(it, "getWalletAddress: failure")
        }

    override suspend fun createWallet(password: String, pin: String, seed: String, walletName: String?): Result<Boolean> =
        runCatching {
            //We need to restart pld before creating a new wallet
            stopPld()
            var wallet = AnodeUtil.DEFAULT_WALLET_NAME
            if (!walletName.isNullOrBlank()) {
                wallet = walletName
            }

            val response = walletAPI.createWallet(password, password, seed.split(" "), "$wallet.db")

            return if (response.message.isNotEmpty()) {
                Result.failure(Exception("Failed to create wallet: ${response.message}"))
            } else {
                val encryptedPassword = AnodeUtil.encrypt(password, pin)
                AnodeUtil.storeWalletPassword(encryptedPassword, wallet)
                if (pin.isNotEmpty()) {
                    AnodeUtil.storeWalletPin(pin, wallet)
                }
                setActiveWallet(wallet)
                Result.success(true)
            }
        }

    override suspend fun recoverWallet(password: String, pin: String, seed: String, seedPassword: String, walletName: String): Result<Boolean> =
        runCatching {
            //We need to restart pld before creating a new wallet
            stopPld()
            var wallet = AnodeUtil.DEFAULT_WALLET_NAME
            if (walletName.isNotEmpty()) {
                wallet = walletName
            }

            val response = walletAPI.recoverWallet(password, seedPassword, seed.split(" "), "$wallet.db")
            return if (response.message.isNotEmpty()) {
                Result.failure(Exception("Failed to recover wallet: ${response.message}"))
            } else {
                val encryptedPassword = AnodeUtil.encrypt(password, pin)
                AnodeUtil.storeWalletPassword(encryptedPassword, wallet)
                if (pin.isNotEmpty()) {
                    AnodeUtil.storeWalletPin(pin, wallet)
                }
                setActiveWallet(wallet)
                Result.success(true)
            }
        }

    override suspend fun unlockWallet(passphrase: String): Result<Boolean> {
        val request = UnlockWalletRequest(passphrase, "$activeWallet.db")
        val response = walletAPI.unlockWalletAPI(request)
        return Result.success(response)
    }

    override suspend fun unlockWalletWithPIN(pin: String): Result<Boolean> {
        val encryptedPassword = AnodeUtil.getWalletPassword(activeWallet)
        val passphrase = AnodeUtil.decrypt(encryptedPassword, pin)
        if (passphrase != null) {
            return unlockWallet(passphrase)
        } else {
            return Result.failure(Exception("Wrong PIN. Cannot decrypt wallet password"))
        }
    }

    override suspend fun createAddress(): Result<WalletAddressCreateResponse> {
        val address = walletAPI.createAddress()
        return Result.success(address)
    }

/*
    override suspend fun getWalletBalances(): Result<WalletAddressBalances> {
        runCatching {
            walletAPI.getWalletBalances(true).getOrThrow()
        }.onSuccess {
            Timber.d("getWalletBalances: success")
        }.onFailure {
            Timber.e(it, "getWalletBalances: failure")
        }
    }
*/

     override suspend fun getWalletTransactions(coinbase: Int, reversed: Boolean, skip: Int, limit: Int, start: Long, end: Long): Result<WalletTransactions> = withContext(Dispatchers.IO) {
        runCatching {
            walletAPI.getWalletTransactions(coinbase, reversed, skip, limit, start, end)
        }.onSuccess {
            Timber.d("getWalletTransactions: success")
        }.onFailure {
            Timber.e(it, "getWalletTransactions: failure")
        }
    }

    override suspend fun getSeed(): Result<String> {
        val response = walletAPI.getWalletSeed()
        return if (response.seed.isNotEmpty()) {
            Result.success(response.seed.joinToString(" "))
        } else {
            Result.failure(Exception("Failed to get seed: ${response.message}"))
        }
    }

    override suspend fun renameWallet(name: String): Result<String?> {
        val walletFile = File("${AnodeUtil.filesDirectory}/pkt/$activeWallet.db")
        walletFile.renameTo(File("${AnodeUtil.filesDirectory}/pkt/$name.db"))
        activeWallet = name
        return Result.success(name)
    }

    override suspend fun checkWalletName(name: String): Result<String?> {
        val existingWallets = getAllWalletNames()
        if (existingWallets.contains(name)) {
            return Result.failure(Exception("Wallet name already exists"))
        } else {
            return Result.success(name)
        }
    }

    override fun deleteWallet(name: String) {
        //Delete saved pin/password
        AnodeUtil.removeEncryptedWalletPreferences(name)
        //Delete Wallet
        AnodeUtil.deleteWallet(name)
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean> {
        val request = ChangePassphraseRequest(oldPassword, newPassword)
        val response = walletAPI.changePassphrase(request)
        return if (response.message.isNotEmpty()) {
            Result.failure(Exception("Failed to change password: ${response.message}"))
        } else {
            AnodeUtil.removeEncryptedWalletPreferences(activeWallet)
            Result.success(true)
        }
    }

    override suspend fun changePin(password: String, pin: String) {
        //Encrypt password using PIN and save it in encrypted shared preferences
        val encryptedPassword = AnodeUtil.encrypt(password, pin)
        AnodeUtil.storeWalletPassword(encryptedPassword,activeWallet)
        //Store PIN in encrypted shared preferences
        AnodeUtil.storeWalletPin(pin,activeWallet)
    }

    override suspend fun generateQr(address: String): Result<Bitmap> {
        val qrgEncoder = QRGEncoder(address, null, QRGContents.Type.TEXT, 600)
        qrgEncoder.colorBlack = Color.WHITE
        qrgEncoder.colorWhite = Color.BLACK
        return try {
            // Getting QR-Code as Bitmap
            val bitmap = qrgEncoder.bitmap
            Result.success(bitmap)
        } catch (e: Exception) {
            Timber.e(e.toString(), "generateQr: failure")
            Result.failure(e)
        }
    }

    override suspend fun checkWalletPassphrase(passphrase: String): Result<Boolean> {
        val request = CheckPassphraseRequest(passphrase)
        val response = walletAPI.checkPassphrase(request)
        return if (response.validPassphrase) {
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid wallet passphrase"))
        }
    }

    override suspend fun sendCoins(fromAddresses: List<String>, amount: Double, toAddress: String): Result<SendTransactionResponse> {
        val request = SendTransactionRequest(toAddress, amount, fromAddresses)
        val response = walletAPI.sendTransaction(request)
        if (!response.message.isNullOrEmpty()) {
            return Result.failure(Exception("Failed to send coins: ${response.message}"))
        } else if (response.txHash.isNotEmpty()){
            return Result.success(response)
        } else {
            return Result.failure(Exception("Failed to send coins"))
        }
    }

    override suspend fun changePassphrase(oldPassphrase: String, newPassphrase: String): Result<Boolean> {
        val request = ChangePassphraseRequest(oldPassphrase, newPassphrase)
        val response = walletAPI.changePassphrase(request)
        if (!response.message.isNullOrEmpty()) {
            return Result.failure(Exception("Failed to change passphrase: ${response.message}"))
        } else {
            return Result.success(true)
        }
    }

    override suspend fun getSecret(): Result<String> {
        val response = walletAPI.getSecret()
        return if (response.secret.isNotEmpty()) {
            Result.success(response.secret)
        } else {
            Result.failure(Exception("Failed to get secret: ${response.message}"))
        }
    }

    override fun isPKTAddressValid(address: String): Result<String> {
        val longRegex = "(pkt1)([a-zA-Z0-9]{59})".toRegex()
        val shortRegex = "(pkt1)([a-zA-Z0-9]{39})".toRegex()
        var trimmedAddress = longRegex.find(address,0)?.value
        if (trimmedAddress.isNullOrEmpty()){
            trimmedAddress = shortRegex.find(address,0)?.value
            if (trimmedAddress.isNullOrEmpty()) {
                return Result.failure(Exception("Invalid PKT address"))
            } else {
                return Result.success(trimmedAddress)
            }
        } else {
            return Result.success(trimmedAddress)
        }
    }

    override suspend fun resyncWallet() {
        walletAPI.resyncWallet()
    }

    override suspend fun getPktToUsd(): Result<Float> {
        val response = walletAPI.getPktToUsd()
        return if (response.isNaN()) {
            Result.failure(Exception("Failed to get PKT to USD conversion rate"))
        } else {
            Result.success(response)
        }
    }

    override suspend fun stopPld() {
        AnodeUtil.stopPld()
        //Wait for pld to restart
        delay(2000)
    }

}
