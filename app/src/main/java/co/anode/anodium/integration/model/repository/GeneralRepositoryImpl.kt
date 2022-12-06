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
}