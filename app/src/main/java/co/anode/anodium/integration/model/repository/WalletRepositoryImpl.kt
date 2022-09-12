package co.anode.anodium.integration.model.repository

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

    override suspend fun getWallets(): Result<List<Addr>> {
        TODO("Not yet implemented")
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

    override suspend fun unlockWallet(passphrase: String, name: String?): Boolean {
        var walletName = ""
        if (!name.isNullOrEmpty()) {
            walletName = name
        }
        val request = UnlockWalletRequest(passphrase, walletName)
        walletAPI.unlockWalletAPI(request)
        return false
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
