package com.pkt.core.presentation.main.settings

import android.net.Uri
import com.pkt.core.R
import com.pkt.core.di.qualifier.Username
import com.pkt.core.di.qualifier.VersionName
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.domain.repository.GeneralRepository
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @VersionName private val versionName: String,
    @Username private val id: String,
    private val walletRepository: WalletRepository,
    private val generalRepository: GeneralRepository,
) : StateViewModel<SettingsState>() {

    var walletUri = Uri.EMPTY

    override fun createInitialState() = SettingsState(
        wallets = walletRepository.getAllWalletNames(),
        walletName = walletRepository.getActiveWallet(),
        id = id,
        version = versionName,
        upgradeChecked = generalRepository.getPreReleaseUpgrade(),
    )

    fun onWalletClick() {
        sendEvent(
            SettingsEvent.OpenChooseWallet(
                currentState.wallets,
                currentState.walletName
            )
        )
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
                sendEvent(SettingsEvent.OpenWalletInfo)
            }
            MenuItem.Type.DATA_CONSENT -> {
                sendEvent(SettingsEvent.OpenConsent(!generalRepository.getDataConsent()))
            }
        }
    }

    fun onRenameClick() {
        sendEvent(SettingsEvent.OpenRenameWallet)
    }

    fun onExportClick() {
        walletUri = walletRepository.getActiveWalletUri()
        sendEvent(SettingsEvent.OpenExportWallet)
    }

    fun onDeleteClick() {
        sendEvent(SettingsEvent.OpenDeleteWallet)
        sendState {
            copy(
                wallets = walletRepository.getAllWalletNames(),
                walletName = walletRepository.getActiveWallet()
            )
        }
    }

    fun onNewWalletClick() {
        sendEvent(SettingsEvent.OpenNewWallet)
    }

    fun onWalletRecoveryClick() {
        sendEvent(SettingsEvent.OpenWalletRecovery)
    }

    fun onConsentResult(value: Boolean) {
        //value return from consent dialog is for opting out
        generalRepository.setDataConsent(!value)
    }

    fun onNewWallet(name: String?, mode: CreateWalletMode) {
        name ?: return
        when(mode) {
            CreateWalletMode.CREATE -> {
                sendEvent(SettingsEvent.OpenCreateWallet(name))
            }
            CreateWalletMode.RECOVER -> {
                sendEvent(SettingsEvent.OpenRecoverWallet(name))
            }
        }
    }

    fun onUpgradeCheckChanged(checked: Boolean) {
        generalRepository.enablePreReleaseUpgrade(checked)
        sendState { copy(upgradeChecked = checked) }
    }

    fun onWalletChanged(walletName: String?) {
        walletName
            ?.takeIf { it != currentState.walletName }
            ?.let { wallet ->
                invokeAction {
                    runCatching {
                        //setting new active wallet, will restart pld
                        walletRepository.setActiveWallet(wallet)
                    }.onSuccess {
                        sendEvent(SettingsEvent.OpenEnterWallet)
                    }.onFailure {
                        sendError(it)
                    }
                }
            }
    }

    fun onWalletRenamed() {
        Timber.d("onWalletRenamed refreshing wallets")
        sendState {
            copy(
                wallets = walletRepository.getAllWalletNames(),
                walletName = walletRepository.getActiveWallet()
            )
        }
    }

    fun onWalletDeleted() {
        //check for remaining wallets
        if (walletRepository.getAllWalletNames().isEmpty()) {
            Timber.d("onWalletDeleted No wallets left, prompting user to create a new one")
            //if no wallets, go to create wallet screen
            sendEvent(SettingsEvent.OpenNewWallet)
        } else {
            Timber.d("onWalletDeleted prompting user to open activewallet")
            sendEvent(SettingsEvent.OpenEnterWallet)
        }
        sendState {
            copy(
                wallets = walletRepository.getAllWalletNames(),
                walletName = walletRepository.getActiveWallet()
            )
        }
    }

    fun onSubmitLogsClick() {
        if (generalRepository.submitErrorLogs()) {
            sendEvent(CommonEvent.Info(R.string.logs_submitted))
        } else {
            sendEvent(CommonEvent.Warning(R.string.logs_submitted_consent))
        }
    }
}
