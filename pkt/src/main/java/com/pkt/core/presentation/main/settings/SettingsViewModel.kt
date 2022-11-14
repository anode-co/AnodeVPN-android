package com.pkt.core.presentation.main.settings

import com.pkt.core.di.qualifier.Username
import com.pkt.core.di.qualifier.VersionName
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.enterwallet.EnterWalletEvent
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @VersionName private val versionName: String,
    @Username private val id: String,
    private val walletRepository: WalletRepository,
) : StateViewModel<SettingsState>() {

    override fun createInitialState() = SettingsState(
        walletName = walletRepository.getActiveWallet(),
        id = id,
        version = versionName
    )

    fun onWalletClick() {
        val wallets = walletRepository.getAllWalletNames()
        if (wallets.size > 1) {
            sendEvent(
                EnterWalletEvent.OpenChooseWallet(wallets, walletRepository.getActiveWallet())
            )
        }
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
        sendEvent(SettingsEvent.OpenNewWallet)
        //sendEvent(SettingsEvent.OpenRecoverWallet(name))
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
