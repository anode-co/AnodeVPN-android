package com.pkt.core.presentation.main.settings

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState

data class SettingsState(
    val walletName: String,
    val id: String,
    val version: String,
    val upgradeChecked: Boolean = false,
    val switchUiChecked: Boolean = false,
) : UiState

sealed class SettingsEvent : UiEvent {

    data class OpenWalletInfo(val address: String) : SettingsEvent()

    object OpenCjdnsInfo : SettingsEvent()

    object OpenShowSeed : SettingsEvent()
    object OpenConsent : SettingsEvent()
    object OpenRenameWallet : SettingsEvent()
    object OpenDeleteWallet : SettingsEvent()
    object OpenNewWallet : SettingsEvent()
    object OpenChangePassword : SettingsEvent()
    object OpenChangePin : SettingsEvent()
    data class OpenCreateWallet(val name: String) : SettingsEvent()
    data class OpenRecoverWallet(val name: String) : SettingsEvent()
}
