package com.pkt.core.presentation.main.settings

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class SettingsState(
    val wallets: List<String>,
    val walletName: String,
    val id: String,
    val version: String,
    val upgradeChecked: Boolean = false,
    val switchUiChecked: Boolean = false,
) : UiState

sealed class SettingsEvent : UiEvent {

    object OpenWalletInfo : SettingsEvent()

    object OpenCjdnsInfo : SettingsEvent()

    object OpenShowSeed : SettingsEvent()
    data class OpenConsent(val optOut: Boolean) : SettingsEvent()
    object OpenRenameWallet : SettingsEvent()
    object OpenExportWallet : SettingsEvent()
    object OpenDeleteWallet : SettingsEvent()
    object OpenNewWallet : SettingsEvent()
    object OpenWalletRecovery : SettingsEvent()
    object OpenChangePassword : SettingsEvent()
    object OpenChangePin : SettingsEvent()
    object OpenEnterWallet : SettingsEvent()
    data class OpenCreateWallet(val name: String) : SettingsEvent()
    data class OpenRecoverWallet(val name: String) : SettingsEvent()
    data class OpenChooseWallet(val wallets: List<String>, val currentWallet: String) : SettingsEvent()
}
