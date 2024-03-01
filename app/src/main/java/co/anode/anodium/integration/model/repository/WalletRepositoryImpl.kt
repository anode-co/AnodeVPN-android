package co.anode.anodium.integration.model.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.core.content.FileProvider
import co.anode.anodium.support.AnodeUtil
import com.pkt.domain.dto.*
import com.pkt.domain.interfaces.WalletAPIService
import com.pkt.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        //Check if wallet file exists, otherwise choose other available wallet
        if (!AnodeUtil.getWalletFiles().contains(activeWallet)) {
            activeWallet = AnodeUtil.getWalletFiles().firstOrNull() ?: AnodeUtil.DEFAULT_WALLET_NAME
        }
        return activeWallet
    }

    override suspend fun setActiveWallet(walletName: String) {
        //delete existing chain back up file when changing active wallet
        if (walletName != activeWallet) {
            AnodeUtil.deleteWalletChainBackupFile()
            //and stop pld
            stopPld()
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
        if (pin == storedPin) {
            return Result.success(true)
        } else {
            return Result.failure(Exception("Pin incorrect"))
        }
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

    override suspend fun getVote(address: String): Result<Vote> {
        val addresses = walletAPI.getWalletBalances(true)
        for (addr in addresses.addrs) {
            if (addr.address == address) {
                return Result.success(addr.vote)
            }
        }
        return Result.failure(Exception("Address not found"))
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
            Timber.d("getWalletBalance: failed")
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
            Timber.d("getWalletAddress: success")
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
                Timber.e("createWallet: failed: ${response.message}")
                Result.failure(Exception("Failed to create wallet: ${response.message}"))
            } else {
                val encryptedPassword = AnodeUtil.encrypt(password, pin)
                AnodeUtil.storeWalletPassword(encryptedPassword, wallet)
                if (pin.isNotEmpty()) {
                    AnodeUtil.storeWalletPin(pin, wallet)
                }
                setActiveWallet(wallet)
                Timber.d("createWallet: success")
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

            val response = walletAPI.recoverWallet(password, seedPassword, seed.lowercase().split(" "), "$wallet.db")
            return if (response.message.isNotEmpty()) {
                Timber.e("Failed to recover wallet: ${response.message}")
                Result.failure(Exception("Failed to recover wallet: ${response.message}"))
            } else {
                val encryptedPassword = AnodeUtil.encrypt(password, pin)
                AnodeUtil.storeWalletPassword(encryptedPassword, wallet)
                if (pin.isNotEmpty()) {
                    AnodeUtil.storeWalletPin(pin, wallet)
                }
                setActiveWallet(wallet)
                Timber.d("Wallet recovered successfully")
                Result.success(true)
            }
        }

    override suspend fun unlockWallet(passphrase: String): Result<Boolean> {
        Timber.d("unlockWallet")
        val request = UnlockWalletRequest(passphrase)
        val response = walletAPI.unlockWalletAPI(request)
        return Result.success(response)
    }

    override suspend fun unlockWalletWithPIN(pin: String): Result<Boolean> {
        Timber.d("unlockWalletWithPIN")
        val encryptedPassword = AnodeUtil.getWalletPassword(activeWallet)
        val passphrase = AnodeUtil.decrypt(encryptedPassword, pin)
        if (passphrase != null) {
            unlockWallet(passphrase).onSuccess {
                Timber.d("unlockWalletWithPIN: success")
                return Result.success(true)
            }.onFailure {
                Timber.e(it, "unlockWalletWithPIN: failure")
                return Result.failure(it)
            }
        } else {
            return Result.failure(Exception("Wrong PIN. Cannot decrypt wallet password"))
        }
        return Result.failure(Exception("Failed to unlock wallet"))
    }

    override suspend fun createAddress(): Result<WalletAddressCreateResponse> {
        Timber.d("createAddress: creating new address")
        val address = walletAPI.createAddress()
        return Result.success(address)
    }

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
            Timber.d("getSeed: success")
            Result.success(response.seed.joinToString(" "))
        } else {
            Timber.d("getSeed: failed")
            Result.failure(Exception("Failed to get seed: ${response.message}"))
        }
    }

    override suspend fun renameWallet(name: String, srcName: String): Result<String?> {
        Timber.d("renameWallet: $name")
        checkWalletName(name).onSuccess {
            if (srcName.isNotEmpty()) {
                activeWallet = srcName
            }
            val walletFile = File("${AnodeUtil.filesDirectory}/pkt/$activeWallet.db")
            walletFile.renameTo(File("${AnodeUtil.filesDirectory}/pkt/$name.db"))
            //update stored PIN
            AnodeUtil.renameEncryptedWalletPreferences(activeWallet,name)
            setActiveWallet(name)
            return Result.success(name)
        }.onFailure {
            Timber.e(it, "renameWallet: failed")
            return Result.failure(Exception("A wallet already exists with this name"))
        }
        return Result.success("")
    }

    override suspend fun checkWalletName(name: String): Result<String?> {
        val existingWallets = getAllWalletNames()
        if (existingWallets.contains(name)) {
            Timber.d("Wallet name already exists")
            return Result.failure(Exception("Wallet name already exists"))
        } else {
            Timber.d("Wallet name is available")
            return Result.success(name)
        }
    }

    override suspend fun deleteWallet(name: String) {
        Timber.d("Deleting wallet $name")
        //Delete saved pin/password
        AnodeUtil.removeEncryptedWalletPreferences(name)
        //Delete Wallet
        AnodeUtil.deleteWallet(name)
        //restart pld
        stopPld()
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean> {
        val request = ChangePassphraseRequest(oldPassword, newPassword)
        val response = walletAPI.changePassphrase(request)
        return if (response.message.isNotEmpty()) {
            Timber.e("Failed to change password: ${response.message}")
            Result.failure(Exception("Failed to change password: ${response.message}"))
        } else {
            Timber.d("changePassword: Success")
            AnodeUtil.removeEncryptedWalletPreferences(activeWallet)
            Result.success(true)
        }
    }

    override suspend fun changePin(password: String, pin: String) {
        Timber.d("changePin")
        //Encrypt password using PIN and save it in encrypted shared preferences
        val encryptedPassword = AnodeUtil.encrypt(password, pin)
        AnodeUtil.storeWalletPassword(encryptedPassword,activeWallet)
        //Store PIN in encrypted shared preferences
        AnodeUtil.storeWalletPin(pin,activeWallet)
    }

    override fun getPin(): String {
        return AnodeUtil.getWalletPin(activeWallet)
    }

    override suspend fun generateQr(address: String): Result<Bitmap> {
        val qrgEncoder = QRGEncoder(address, null, QRGContents.Type.TEXT, 600)
        qrgEncoder.colorBlack = Color.WHITE
        qrgEncoder.colorWhite = Color.BLACK
        return try {
            // Getting QR-Code as Bitmap
            val bitmap = qrgEncoder.bitmap
            Timber.d("generateQr: success")
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
            Timber.d("checkWalletPassphrase: success")
            Result.success(true)
        } else {
            Timber.e("checkWalletPassphrase: failure")
            Result.failure(Exception("Invalid wallet passphrase"))
        }
    }

    override suspend fun sendCoins(fromAddresses: List<String>, amount: Double, toAddress: String): Result<SendTransactionResponse> {
        val request = SendTransactionRequest(toAddress, amount, fromAddresses)
        val response = walletAPI.sendTransaction(request)
        if (!response.message.isNullOrEmpty()) {
            Timber.e("sendCoins: Failed: ${response.message}")
            return Result.failure(Exception("Failed to send coins: ${response.message}"))
        } else if (response.txHash.isNotEmpty()){
            Timber.d("sendCoins: success")
            return Result.success(response)
        } else {
            Timber.e("sendCoins: Failed, empty response")
            return Result.failure(Exception("Failed to send coins"))
        }
    }

    override suspend fun sendVote(fromAddress: String, voteFor: String, isCandidate: Boolean): Result<SendVoteResponse> {
        val request = SendVoteRequest(voteFor, fromAddress, isCandidate)
        val response = walletAPI.sendVote(request)
        if (response.message.isNotEmpty()) {
            Timber.e("sendVote: Failed: ${response.message}")
            return Result.failure(Exception("Failed to send vote: ${response.message}"))
        } else if (response.txHash.isNotEmpty()){
            Timber.d("sendVote: success")
            return Result.success(response)
        } else {
            Timber.e("sendVote: Failed, empty response")
            return Result.failure(Exception("Failed to send vote"))
        }
    }

    override suspend fun createTransaction(fromAddresses: List<String>, amount: Double, toAddress: String): Result<CreateTransactionResponse> {
        val request = CreateTransactionRequest(toAddress, amount, fromAddresses, true)
        val response = walletAPI.createTransaction(request)
        if (!response.message.isNullOrEmpty()) {
            Timber.e("createTransaction: Failed: ${response.message}")
            return Result.failure(Exception("Failed to create transaction: ${response.message}"))
        } else if (response.transaction.isNotEmpty()){
            Timber.d("createTransaction: success")
            return Result.success(response)
        } else {
            Timber.e("createTransaction: Failed, empty response")
            return Result.failure(Exception("Failed to send coins"))
        }
    }

    override suspend fun changePassphrase(oldPassphrase: String, newPassphrase: String): Result<Boolean> {
        val request = ChangePassphraseRequest(oldPassphrase, newPassphrase)
        val response = walletAPI.changePassphrase(request)
        if (!response.message.isNullOrEmpty()) {
            Timber.e("changePassphrase: Failed: ${response.message}")
            return Result.failure(Exception("Failed to change passphrase: ${response.message}"))
        } else {
            Timber.d("changePassphrase: Success")
            return Result.success(true)
        }
    }

    override suspend fun getSecret(): Result<String> {
        val response = walletAPI.getSecret()
        return if (response.secret.isNotEmpty()) {
            Timber.d("getSecret: Success")
            Result.success(response.secret)
        } else {
            Timber.e("getSecret: Failure: ${response.message}")
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
                Timber.e("isPKTAddressValid: Invalid address")
                return Result.failure(Exception("Invalid PKT address"))
            } else {
                Timber.d("isPKTAddressValid: Valid short address")
                return Result.success(trimmedAddress)
            }
        } else {
            Timber.d("isPKTAddressValid Success")
            return Result.success(trimmedAddress)
        }
    }

    override suspend fun resyncWallet() {
        Timber.d("resyncWallet: called")
        walletAPI.resyncWallet()
    }

    override suspend fun getPktToUsd(): Result<Float> {
        val response = walletAPI.getPktToUsd()
        return if (response.isNaN()) {
            Timber.d("getPktToUsd Failure")
            Result.failure(Exception("Failed to get PKT to USD conversion rate"))
        } else {
            Timber.d("getPktToUsd Success")
            Result.success(response)
        }
    }

    override suspend fun stopPld() {
        Timber.d("stopPld: stopping pld")
        AnodeUtil.stopPld()
        //Wait for pld to restart
        delay(2000)
    }

    override fun getActiveWalletUri(): Uri? {
        return AnodeUtil.context?.let { FileProvider.getUriForFile(it, AnodeUtil.ApplicationID +".provider", File("${AnodeUtil.filesDirectory}/pkt/$activeWallet.db")) }
    }

    override suspend fun decodeTransaction(binTx: String): String {
        return walletAPI.decodeTransaction(binTx)
    }
}
