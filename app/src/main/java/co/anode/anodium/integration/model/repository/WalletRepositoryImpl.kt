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
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor() : WalletRepository {
    private val walletAPI = WalletAPIService()
    private var activeWallet = "wallet"

    override suspend fun getAllWalletNames(): Result<List<String>> {
        return Result.success(AnodeUtil.getWalletFiles())
    }

    override suspend fun getActiveWallet(): Result<String> {
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.getString("activeWallet", "wallet")?.let {
            activeWallet = it
        }
        return Result.success(activeWallet)
    }

    override suspend fun setActiveWallet(walletName: String) {
        AnodeUtil.context?.getSharedPreferences(AnodeUtil.ApplicationID, Context.MODE_PRIVATE)?.edit()?.putString("activeWallet", walletName)?.apply()
    }

    override suspend fun getWalletAddress(): Result<String> = withContext(Dispatchers.IO){
        runCatching {
            val addresses = walletAPI.getWalletBalances(true)
            //Return the balance of address
            var address = ""
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
            return@runCatching address
        }.onSuccess {
            Timber.d("getWalletAddress: success")
        }.onFailure {
            Timber.e(it, "getWalletAddress: failure")
        }
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
    override suspend fun getTotalWalletBalance(): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            val addresses = walletAPI.getWalletBalances(true)
            //Return the balance of address
            var balance = 0.0
            for (i in 0 until addresses.addrs.size) {
                balance = addresses.addrs[i].total
            }
            return@runCatching balance
        }.onSuccess {
            Timber.d("getTotalWalletBalance: success")
        }.onFailure {
            Timber.e(it, "getTotalWalletBalance: failure")
        }
    }

    //Get wallet balance, if address is empty then return total balance of all addresses
    override suspend fun getWalletBalance(address: String): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            val addresses = walletAPI.getWalletBalances(true)
            //Return the balance of address
            var balance = 0.0
            for (i in 0 until addresses.addrs.size) {
                if (address.isEmpty()) {
                    balance += addresses.addrs[i].total
                } else if (addresses.addrs[i].address == address) {
                    balance = addresses.addrs[i].total
                }
            }
            return@runCatching balance
        }.onSuccess {
            Timber.d("getWalletBalance: success")
        }.onFailure {
            Timber.e(it, "getWalletBalance: failure")
        }
    }

    override suspend fun getWalletInfo(): Result<WalletInfo> {
        val walletInfo = walletAPI.getWalletInfo()
        if (walletInfo.wallet == null) {

        }
        return Result.success(walletInfo)
    }

    override suspend fun generateSeed(password: String, pin: String): Result<String> {
        val response = walletAPI.createSeed(password)

        if (response.seed.isNotEmpty()) {
            return Result.success(response.seed.joinToString(" "))
        } else {
            return Result.failure(Exception("Failed to generate seed: ${response.message}"))
        }
    }

    override suspend fun createWallet(password: String, pin: String, seed: String, walletName: String?): Result<Boolean> =
        runCatching {
            var wallet = "wallet" // Default wallet name
            if (!walletName.isNullOrBlank()) {
                wallet = walletName
            }
            setActiveWallet(wallet)

            val response = walletAPI.createWallet(password, password, seed.split(" "), "$activeWallet.db")

            return if (response.message.isNotEmpty()) {
                Result.failure(Exception("Failed to create wallet: ${response.message}"))
            } else {
                val encryptedPassword = AnodeUtil.encrypt(password, pin)
                AnodeUtil.storeWalletPassword(encryptedPassword, wallet)
                if (pin.isNotEmpty()) {
                    AnodeUtil.storeWalletPin(pin, wallet)
                }
                Result.success(true)
            }
        }

    override suspend fun recoverWallet(password: String, seed: String, seedPassword: String, walletName: String): Result<Boolean> =
        runCatching {
            var wallet = "wallet" //Default wallet name
            if (walletName.isNotEmpty()) {
                wallet = walletName
            }
            setActiveWallet(wallet)
            val response = walletAPI.recoverWallet(password, seedPassword, seed.split(" "), "$activeWallet.db")
            return if (response.message.isNotEmpty()) {
                Result.failure(Exception("Failed to recover wallet: ${response.message}"))
            } else {
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
            val request = UnlockWalletRequest(passphrase, "$activeWallet.db")
            return Result.success(walletAPI.unlockWalletAPI(request))
        } else {
            return Result.failure(Exception("Wrong PIN. Cannot decrypt wallet password"))
        }
    }

    override suspend fun createAddress(): Result<WalletAddressCreateResponse> {
        val address = walletAPI.createAddress()
        return Result.success(address)
    }

    override suspend fun getWalletBalances(): Result<WalletAddressBalances> = withContext(Dispatchers.IO) {
        runCatching {
            walletAPI.getWalletBalances(true)
        }.onSuccess {
            Timber.d("getWalletBalances: success")
        }.onFailure {
            Timber.e(it, "getWalletBalances: failure")
        }
    }

    override suspend fun getWalletTransactions(): Result<WalletTransactions> = withContext(Dispatchers.IO) {
        runCatching {
            walletAPI.getWalletTransactions(1, false, 0, 10)
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
        val existingWallets = getAllWalletNames().getOrThrow()
        if (existingWallets.contains(name)) {
            return Result.failure(Exception("Wallet name already exists"))
        } else {
            return Result.success(name)
        }
    }

    override suspend fun deleteWallet(name: String): Result<String?> {
        TODO("Not yet implemented")
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean> {
        val request = ChangePassphraseRequest(oldPassword, newPassword)
        val response = walletAPI.changePassphrase(request)
        return if (response.message.isNotEmpty()) {
            Result.failure(Exception("Failed to change password: ${response.message}"))
        } else {
            //TODO: remove store PIN and encrypted password
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
        var qrgEncoder = QRGEncoder(address, null, QRGContents.Type.TEXT, 600)
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

    override suspend fun sendCoins(fromAddresses: List<String>, amount: Long, toAddress: String): Result<SendTransactionResponse> {
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

}
