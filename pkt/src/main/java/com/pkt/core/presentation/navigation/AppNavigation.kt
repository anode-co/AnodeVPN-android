package com.pkt.core.presentation.navigation

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiNavigation

sealed class AppNavigation : UiNavigation {

    object NavigateBack : AppNavigation()

    object OpenCjdnsInfo : AppNavigation()
    data class OpenWalletInfo(val address: String) : AppNavigation()
    data class OpenCreateWallet(val name: String? = null) : AppNavigation()
    object OpenRecoverWallet : AppNavigation()
    object OpenMain : AppNavigation()
    data class OpenSendTransaction(val fromAddress: String) : AppNavigation()
    data class OpenSendConfirm(val fromaddress: String, val toaddress:String, val amount: Double, val maxAmount: Boolean) : AppNavigation()
    data class OpenSendSuccess(val transactionId: String) : AppNavigation()
    object OpenVpnExits : AppNavigation()
    object OpenChangePassword : AppNavigation()
    object OpenChangePin : AppNavigation()
}

interface InternalNavigation : UiNavigation {
    val navDirections: NavDirections
}
