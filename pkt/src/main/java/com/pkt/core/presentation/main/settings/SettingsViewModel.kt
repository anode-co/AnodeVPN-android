package com.pkt.core.presentation.main.settings

import androidx.appcompat.app.AppCompatActivity
import com.pkt.core.di.qualifier.VersionName
import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @VersionName private val versionName: String,
) : StateViewModel<SettingsState>() {

    override fun createInitialState() = SettingsState(
        walletName = "wallet",
        id = "",
        version = versionName
    )

    fun onWalletClick() {
        // TODO
    }

    fun onMenuItemClick(item: MenuItem) {
        when (item.type) {
            MenuItem.Type.CHANGE_PASSWORD -> {
                sendEvent(SettingsEvent.OpenChangePassword)
            }
            MenuItem.Type.CHANGE_PIN -> {
                sendEvent(SettingsEvent.OpenChangePin)
            }
            MenuItem.Type.SHOW_SEED -> {
                sendEvent(SettingsEvent.OpenShowSeed)
            }
            MenuItem.Type.CJDNS_INFO -> {
                sendEvent(SettingsEvent.OpenCjdnsInfo)
            }
            MenuItem.Type.WALLET_INFO -> {
                sendEvent(SettingsEvent.OpenWalletInfo("pkt1q282zvfztp00nrelpw0lmy7pwz0lvz6vlmzwgzm"))
            }
            MenuItem.Type.DATA_CONSENT -> {
                sendEvent(SettingsEvent.OpenConsent)
            }
        }
    }

    fun onRenameClick() {
        sendEvent(SettingsEvent.OpenRenameWallet)
    }

    fun onExportClick() {
        // TODO
    }

    fun onDeleteClick() {
        sendEvent(SettingsEvent.OpenDeleteWallet)
    }

    fun onNewWalletClick() {
        sendEvent(SettingsEvent.OpenNewWallet)
    }

    fun onWalletRecoveryClick() {
        sendEvent(SettingsEvent.OpenRecoverWallet)
    }

    fun onConsentResult(success: Boolean) {
        //TODO: save in prefs
        //context?.getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)?.edit()?.putBoolean("DataConsent", success)?.apply()
    }

    fun onNewWallet(name: String?) {
        name ?: return

        sendEvent(SettingsEvent.OpenCreateWallet(name))
    }
}
