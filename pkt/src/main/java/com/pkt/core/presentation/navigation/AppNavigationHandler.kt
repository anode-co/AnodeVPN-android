package com.pkt.core.presentation.navigation

import androidx.fragment.app.Fragment
import com.pkt.core.presentation.common.state.UiNavigation
import com.pkt.core.presentation.common.state.navigation.NavigationHandler

abstract class AppNavigationHandler : NavigationHandler {

    override fun handleNavigation(fragment: Fragment, navigation: UiNavigation) {
        when (navigation) {
            is AppNavigation -> {
                when (navigation) {
                    AppNavigation.NavigateBack -> navigateBack(fragment)
                    is AppNavigation.OpenCjdnsInfo -> openCjdnsInfo(fragment, navigation.address)
                    is AppNavigation.OpenWalletInfo -> openWalletInfo(fragment, navigation.address)
                }
            }

            else -> {
                throw IllegalArgumentException("Unknown UiNavigation: $navigation")
            }
        }
    }

    abstract fun navigateBack(fragment: Fragment)
    abstract fun openCjdnsInfo(fragment: Fragment, address: String)
    abstract fun openWalletInfo(fragment: Fragment, address: String)
}
