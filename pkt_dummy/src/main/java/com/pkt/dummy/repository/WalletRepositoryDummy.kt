package com.pkt.dummy.repository

import android.content.Context
import com.pkt.domain.dto.Addr
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.WalletInfo
import com.pkt.domain.repository.WalletRepository
import com.pkt.dummy.AddrMapper
import com.pkt.dummy.CjdnsInfoMapper
import com.pkt.dummy.R
import com.pkt.dummy.WalletInfoMapper
import com.pkt.dummy.dto.*
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

    override suspend fun getWallets(): Result<List<Addr>> = withContext(Dispatchers.IO) {
        runCatching {
            delay(1000L)
            context.resources.openRawResource(R.raw.wallets).use {
                json.decodeFromStream<WalletAddressBalancesDummy>(it)
            }.addrs.map { addrMapper.map(it) }
        }
    }

    override suspend fun getWalletBalance(address: String): Result<Double> {
        return getWallets()
            .mapCatching { list ->
                list.find { it.address == address }!!.total
            }
    }

    override suspend fun getWalletInfo(address: String): Result<WalletInfo> = withContext(Dispatchers.IO) {
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
}
