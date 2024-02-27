package co.anode.anodium.integration.model.repository

import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import com.pkt.domain.repository.GeneralRepository
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneralRepositoryImpl @Inject constructor() : GeneralRepository {
    override fun submitErrorLogs(): Boolean {
        AnodeClient.storeError(null,"other", Throwable("User submitted logs"))
        AnodeClient.PostLogs()
        return getDataConsent()
    }

    override fun enablePreReleaseUpgrade(value: Boolean) {
        AnodeUtil.setPreReleaseUpgrade(value)
    }

    override fun getPreReleaseUpgrade(): Boolean {
        return AnodeUtil.getPreReleaseUpgrade()
    }

    override fun removePIN(walletName: String) {
        Timber.d("removePIN $walletName")
        AnodeUtil.removeEncryptedWalletPreferences(walletName)
    }

    override fun restartApplication() {
        Timber.d("restartApplication")
        Thread.sleep(500)
        AnodeUtil.restartApp()
    }

    override fun hasInternetConnection(): Boolean {
        return AnodeUtil.internetConnection()
    }

    override fun getDataConsent(): Boolean {
        return AnodeUtil.getDataConsentFromSharedPrefs()
    }

    override fun setDataConsent(value: Boolean) {
        AnodeUtil.setDataConsentToSharedPrefs(value)
    }

    override fun saveGetInfoHtml(html: String) {
        val htmlFile = File("${AnodeUtil.filesDirectory}/getinfo.html")
        htmlFile.writeText(html, Charsets.UTF_8)
    }

    override fun savePldLogHtml(html: String) {
        val htmlFile = File("${AnodeUtil.filesDirectory}/pldlog.html")
        htmlFile.writeText(html, Charsets.UTF_8)
    }

    override fun getWalletInfoUrl(): String {
        return "file:///${AnodeUtil.filesDirectory}/getinfo.html"
    }

    override fun getPldLogUrl(): String {
        return "file:///${AnodeUtil.filesDirectory}/pldlog.html"
    }

    override fun setShowInactiveServers(value: Boolean) {
        AnodeUtil.setShowInactiveServers(value)
    }

    override fun getShowInactiveServers(): Boolean {
        return AnodeUtil.getShowInactiveServers()
    }

    override fun setPremiumEndTime(value: Long, server: String) {
        AnodeUtil.setPremiumEndTime(value, server)
    }

    override fun getPremiumEndTime(server: String): Long {
        return AnodeUtil.getPremiumEndTime(server)
    }

    override suspend fun createPldWallet(password: String, pin: String, wallet: String): Result<String> {
        runCatching {
            val seed = AnodeUtil.createPldWallet(password, wallet)
            if (seed.isEmpty()) {
                return Result.failure(Exception("Wallet creation failed"))
            }
            val encryptedPassword = AnodeUtil.encrypt(password, pin)
            AnodeUtil.storeWalletPassword(encryptedPassword, wallet)
            if (pin.isNotEmpty()) {
                AnodeUtil.storeWalletPin(pin, wallet)
            }

            Timber.d("createWallet: success")
            return Result.success(seed)
        }
        return Result.failure(Exception("Wallet creation failed"))
    }

    override fun launchPLD() {
        AnodeUtil.launchPld("")
    }
}