package com.pkt.core.presentation.navigation

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiNavigation

sealed class AppNavigation : UiNavigation {

    object NavigateBack : AppNavigation()

    data class OpenCjdnsInfo(val address: String) : AppNavigation()
    data class OpenWalletInfo(val address: String) : AppNavigation()
    object OpenCreateWallet : AppNavigation()
    object OpenRecoverWallet : AppNavigation()
    object OpenMain : AppNavigation()
}

interface InternalNavigation : UiNavigation {
    val navDirections: NavDirections
}
