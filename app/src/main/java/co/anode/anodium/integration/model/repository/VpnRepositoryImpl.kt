package co.anode.anodium.integration.model.repository

import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnState
import com.pkt.domain.repository.VpnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepositoryImpl @Inject constructor() : VpnRepository {

    override val vpnListFlow: Flow<List<Vpn>>
        get() = TODO("Not yet implemented")

    override val currentVpnFlow: Flow<Vpn?>
        get() = TODO("Not yet implemented")

    override val vpnStateFlow: Flow<VpnState>
        get() = TODO("Not yet implemented")

    override val startConnectionTime: Long
        get() = TODO("Not yet implemented")

    override suspend fun fetchVpnList(force: Boolean): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun setCurrentVpn(name: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun connect(): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect(): Result<Boolean> {
        TODO("Not yet implemented")
    }
}
