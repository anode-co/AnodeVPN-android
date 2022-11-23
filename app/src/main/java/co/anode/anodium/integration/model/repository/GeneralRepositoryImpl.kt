package co.anode.anodium.integration.model.repository

import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import com.pkt.domain.repository.GeneralRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneralRepositoryImpl @Inject constructor() : GeneralRepository {
    override fun submitErrorLogs() {
        AnodeClient.storeError(null,"other", Throwable("User submitted logs"))
        AnodeClient.PostLogs()
    }

    override fun enablePreReleaseUpgrade(value: Boolean) {
        AnodeUtil.setPreReleaseUpgrade(value)
    }

    override fun getPreReleaseUpgrade(): Boolean {
        return AnodeUtil.getPreReleaseUpgrade()
    }

    override fun enableNewUI(value: Boolean) {
        AnodeUtil.setUseNewUi(value)
    }

    override fun getNewUI(): Boolean {
        return AnodeUtil.getUseNewUi()
    }

    override fun removePIN(walletName: String) {
        AnodeUtil.removeEncryptedWalletPreferences(walletName)
    }

}