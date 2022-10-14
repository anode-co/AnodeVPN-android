package co.anode.anodium.integration.model.repository

import co.anode.anodium.support.AnodeUtil
import com.pkt.domain.dto.*
import com.pkt.domain.interfaces.WalletAPIService
import com.pkt.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
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
        TODO("Not yet implemented")
        //get activeWallet from sharedPreferences or DataStore
    }

    override suspend fun setActiveWallet(walletName: String) {
        TODO("Not yet implemented")
        //set activeWallet to sharedPreferences or DataStore
    }

    override suspend fun getWalletAddress(): Result<String> {
        TODO("Not yet implemented")
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

    //Get wallet balance, if address is empty then return total balance of all addresses
    override suspend fun getWalletBalance(address: String): Result<Double> = withContext(Dispatchers.IO){
        runCatching {
            val addresses = walletAPI.getWalletBalances(true)
            //Return the balance of address
            var balance = 0.0
            for(i in 0 until addresses.addrs.size) {
                if (address.isEmpty()) {
                    balance += addresses.addrs[i].total
                } else if (addresses.addrs[i].address == address) {
                    balance = addresses.addrs[i].total
                }
            }
            return@runCatching balance
        }.onSuccess {
            Timber.d("getCurrentAddress: success")
        }.onFailure {
            Timber.e(it, "getCurrentAddress: failure")
        }
    }

    override suspend fun getWalletInfo(): Result<WalletInfo>  = withContext(Dispatchers.IO)  {
        kotlin.runCatching {
            walletAPI.getWalletInfo()
        }.onSuccess {
            Timber.d("getWalletIfo: success")
        }.onFailure {
            Timber.e(it, "getWalletInfo : failure")
        }
    }

    override suspend fun getCjdnsInfo(address: String): Result<CjdnsInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun generateSeed(password: String, pin: String): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun createWallet(password: String, pin: String, seed: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun recoverWallet(password: String, seed: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun unlockWallet(passphrase: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val request = UnlockWalletRequest(passphrase, activeWallet)
            walletAPI.unlockWalletAPI(request)
        }.onSuccess {
            Timber.d("unlockWalletAPI: success")
        }.onFailure {
            Timber.e(it, "unlockWalletAPI : failure")
        }
    }

    override suspend fun unlockWalletWithPIN(pin: String): Result<Boolean> {
        val encryptedPassword = AnodeUtil.getWalletPassword(activeWallet)
        val passphrase = AnodeUtil.decrypt(encryptedPassword, pin).toString()
        val request = UnlockWalletRequest(passphrase, "$activeWallet.db")
        return Result.success(walletAPI.unlockWalletAPI(request))
    }

    override suspend fun createAddress(): String {
        return walletAPI.createAddress()
    }

    override suspend fun getWalletBalances(): Result<WalletAddressBalances> = withContext(Dispatchers.IO){
        runCatching {
            walletAPI.getWalletBalances(true)
        }.onSuccess {
            Timber.d("getWalletBalances: success")
        }.onFailure {
            Timber.e(it, "getWalletBalances: failure")
        }
    }

    override suspend fun getCurrentAddress(): Result<String> = withContext(Dispatchers.IO){
        runCatching {
            val addresses = walletAPI.getWalletBalances(true)
            //Get the address with the heighest balance
            var balance = -1.0
            var address = ""
            for(i in 0 until addresses.addrs.size) {
                if (addresses.addrs[i].total > balance) {
                    balance = addresses.addrs[i].total
                    address = addresses.addrs[i].address
                }
            }
            return@runCatching address
        }.onSuccess {
            Timber.d("getCurrentAddress: success")
        }.onFailure {
            Timber.e(it, "getCurrentAddress: failure")
        }
    }

    override suspend fun getWalletTransactions(): Result<WalletTransactions> = withContext(Dispatchers.IO){
        runCatching {
            walletAPI.getWalletTransactions(1, false, 0, 10)
        }.onSuccess {
            Timber.d("getWalletTransactions: success")
        }.onFailure {
            Timber.e(it, "getWalletTransactions: failure")
        }
    }

}
