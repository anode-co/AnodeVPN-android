package com.pkt.dummy.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pkt.domain.dto.*
import com.pkt.domain.repository.WalletRepository
import com.pkt.dummy.AddrMapper
import com.pkt.dummy.CjdnsInfoMapper
import com.pkt.dummy.R
import com.pkt.dummy.WalletInfoMapper
import com.pkt.dummy.dto.CjdnsInfoDummy
import com.pkt.dummy.dto.WalletAddressBalancesDummy
import com.pkt.dummy.dto.WalletInfoDummy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.mapstruct.factory.Mappers

@OptIn(ExperimentalSerializationApi::class)
class WalletRepositoryDummy constructor(
    private val context: Context,
) : WalletRepository {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    private val addrMapper by lazy {
        Mappers.getMapper(AddrMapper::class.java)
    }

    private val walletInfoMapper by lazy {
        Mappers.getMapper(WalletInfoMapper::class.java)
    }

    private val cjdnsInfoMapper by lazy {
        Mappers.getMapper(CjdnsInfoMapper::class.java)
    }

    private var activeWallet = "My Wallet 1"

    override suspend fun getAllWalletNames(): Result<List<String>> =
        Result.success(listOf("My Wallet 1", "My Wallet 2", "My Wallet 3"))

    override suspend fun getActiveWallet(): Result<String> = Result.success(activeWallet)

    override suspend fun setActiveWallet(walletName: String) {
        activeWallet = walletName
    }

    override suspend fun getWalletAddress(): Result<String> =
        Result.success("pkt1q282zvfztp00nrelpw0lmy7pwz0lvz6vlmzwgzm")

    private suspend fun getWallets(): Result<List<Addr>> = withContext(Dispatchers.IO) {
        runCatching {
            delay(1000L)
            context.resources.openRawResource(R.raw.wallets).use {
                json.decodeFromStream<WalletAddressBalancesDummy>(it)
            }.addrs.map { addrMapper.map(it) }
        }
    }

    override suspend fun isPinAvailable(): Result<Boolean> {
        delay(1000)
        return if (activeWallet != "My Wallet 3") {
            Result.success(true)
        } else {
            Result.success(false)
        }
    }

    override suspend fun checkPin(pin: String): Result<Boolean> = runCatching {
        delay(1000L)
        return if (pin == "1111") {
            Result.success(true)
        } else {
            Result.success(false)
        }
    }

    override suspend fun getWalletBalance(address: String): Result<Double> {
        return getWallets()
            .mapCatching { list ->
                list.find { it.address == address }!!.total
            }
    }

    override suspend fun getTotalWalletBalance(): Result<Double> =
        getWalletBalance("pkt1q282zvfztp00nrelpw0lmy7pwz0lvz6vlmzwgzm")

    override suspend fun getWalletInfo(): Result<WalletInfo> = withContext(Dispatchers.IO) {
        runCatching {
            delay(1000L)
            val info = context.resources.openRawResource(R.raw.wallet_info_1).use {
                json.decodeFromStream<WalletInfoDummy>(it)
            }
            walletInfoMapper.map(info)
        }
    }

    override suspend fun getCjdnsInfo(address: String): Result<CjdnsInfo> = withContext(Dispatchers.IO) {
        runCatching {
            delay(1000L)
            val info = context.resources.openRawResource(R.raw.cjdns_info).use {
                json.decodeFromStream<CjdnsInfoDummy>(it)
            }
            cjdnsInfoMapper.map(info)
        }
    }

    override suspend fun generateSeed(password: String, pin: String) = withContext(Dispatchers.IO) {
        delay(5000L)
        Result.success("Tail net similar exercise scan sting buddy oil during museum outside cluster extra aim")
    }

    override suspend fun createWallet(password: String, pin: String, seed: String, walletName: String?) =
        withContext(Dispatchers.IO) {
            delay(5_000L)
            Result.success(true)
        }

    override suspend fun recoverWallet(password: String, seed: String, seedPassword: String, walletName: String): Result<Boolean> {
        delay(1_000L)
        return Result.success(true)
    }

    override suspend fun unlockWallet(passphrase: String): Result<Boolean> {
        delay(1000L)
        return if (passphrase == "qwerty") {
            Result.success(true)
        } else {
            Result.success(false)
        }
    }

    override suspend fun unlockWalletWithPIN(pin: String): Result<Boolean> {
        delay(1000L)
        return if (pin == "1111") {
            Result.success(true)
        } else {
            Result.success(false)
        }
    }

    override suspend fun createAddress(): String {
        TODO("Not yet implemented")
    }

    override suspend fun getWalletBalances(): Result<WalletAddressBalances> {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentAddress(): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getWalletTransactions(): Result<WalletTransactions> {
        TODO("Not yet implemented")
    }

    override suspend fun send(address: String, amount: Double): Result<SendResponse> = withContext(Dispatchers.IO) {
        delay(1000L)
        Result.success(
            SendResponse(
                transactionId = "0bd1574c52a1621e4b522e9a45226eff2",
                address = address,
                blockNumber = 1234567,
                amount = amount,
                timestamp = "2022-07-06 11:19:06 +0300 EEST"
            )
        )
    }

    override suspend fun getSeed(): Result<String> =
        Result.success("Tail net similar exercise scan sting buddy oil during museum outside cluster extra aim")

    override suspend fun renameWallet(name: String): Result<String?> {
        delay(1000)
        return Result.success(
            if (name.any { !it.isLetterOrDigit() && !it.isWhitespace() }) {
                "Name contains invalid characters"
            } else {
                null
            }
        )
    }

    override suspend fun checkWalletName(name: String): Result<String?> {
        delay(1000)
        return Result.success(
            if (name.any { !it.isLetterOrDigit() && !it.isWhitespace() }) {
                "Name contains invalid characters"
            } else {
                null
            }
        )
    }

    override suspend fun deleteWallet(name: String): Result<String?> {
        delay(1000)
        return Result.success(
            if (name == "My Wallet 1") {
                null
            } else {
                "Invalid name"
            }
        )
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        delay(1000L)
        return Result.success(Unit)
    }

    override suspend fun changePin(password: String, pin: String): Result<Unit> {
        delay(1000L)
        return Result.success(Unit)
    }

    override suspend fun generateQr(): Result<Bitmap> = withContext(Dispatchers.IO) {
        delay(1000L)
        Result.success(BitmapFactory.decodeResource(context.resources, R.drawable.qr_code))
    }
}
