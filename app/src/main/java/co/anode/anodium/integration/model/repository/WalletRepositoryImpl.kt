package co.anode.anodium.integration.model.repository

import com.pkt.domain.dto.Addr
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.WalletInfo
import com.pkt.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor() : WalletRepository {

    override suspend fun getWallets(): Result<List<Addr>> {
        TODO("Not yet implemented")
    }

    override suspend fun getWalletBalance(address: String): Result<Double> {
        TODO("Not yet implemented")
    }

    override suspend fun getWalletInfo(address: String): Result<WalletInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getCjdnsInfo(address: String): Result<CjdnsInfo> {
        TODO("Not yet implemented")
    }
}
