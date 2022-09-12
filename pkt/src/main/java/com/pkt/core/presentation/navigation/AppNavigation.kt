package com.pkt.core.presentation.navigation

import com.pkt.core.presentation.common.state.UiNavigation

sealed class AppNavigation : UiNavigation {

    object NavigateBack : AppNavigation()

    data class OpenCjdnsInfo(val address: String) : AppNavigation()
    data class OpenWalletInfo(val address: String) : AppNavigation()
}
