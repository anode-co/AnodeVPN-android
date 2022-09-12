package com.pkt.domain.repository

import com.pkt.domain.dto.Addr
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.WalletInfo

interface WalletRepository {
    suspend fun getWallets(): Result<List<Addr>>
    suspend fun getWalletBalance(address: String): Result<Double>
    suspend fun getWalletInfo(address: String): Result<WalletInfo>
    suspend fun getCjdnsInfo(address: String): Result<CjdnsInfo>
}
