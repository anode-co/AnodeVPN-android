package com.pkt.core.presentation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.pkt.core.presentation.common.state.UiNavigation
import com.pkt.core.presentation.common.state.navigation.NavigationHandler
import com.pkt.core.presentation.createwallet.CreateWalletMode

abstract class AppNavigationHandler : NavigationHandler {

    override fun handleNavigation(fragment: Fragment, navigation: UiNavigation) {
        when (navigation) {
            is AppNavigation -> {
                when (navigation) {
                    AppNavigation.NavigateBack -> navigateBack(fragment)
                    is AppNavigation.OpenCjdnsInfo -> openCjdnsInfo(fragment)
                    is AppNavigation.OpenWalletInfo -> openWalletInfo(fragment, navigation.address)
                    is AppNavigation.OpenCreateWallet -> openCreateWallet(fragment, navigation.name, navigation.mode)
                    is AppNavigation.OpenRecoverWallet -> openRecoverWallet(fragment, navigation.name)
                    AppNavigation.OpenMain -> openMain(fragment)
                    is AppNavigation.OpenSendConfirm -> openSendConfirm(
                        fragment,
                        navigation.fromaddress,
                        navigation.toaddress,
                        navigation.amount,
                        navigation.maxAmount
                    )
                    is AppNavigation.OpenSendSuccess -> openSendSuccess(fragment, navigation.transactionId)
                    AppNavigation.OpenVpnExits -> openVpnExits(fragment)
                    AppNavigation.OpenChangePassword -> openChangePassword(fragment)
                    AppNavigation.OpenChangePin -> openChangePin(fragment)
                    is AppNavigation.OpenSendTransaction -> openSendTransaction(fragment, navigation.fromAddress)
                    AppNavigation.OpenEnterWallet -> openEnterWallet(fragment)
                    AppNavigation.OpenStart -> openStart(fragment)
                }
            }

            is InternalNavigation -> {
                navigate(fragment, navigation.navDirections)
            }

            else -> {
                throw IllegalArgumentException("Unknown UiNavigation: $navigation")
            }
        }
    }

    private fun navigate(fragment: Fragment, navDirections: NavDirections) {
        fragment.findNavController()
            .takeIf { it.currentDestination?.getAction(navDirections.actionId) != null }
            ?.navigate(navDirections)
    }

    abstract fun navigateBack(fragment: Fragment)
    abstract fun openCjdnsInfo(fragment: Fragment)
    abstract fun openWalletInfo(fragment: Fragment, address: String)
    abstract fun openCreateWallet(fragment: Fragment, name: String?, mode: CreateWalletMode)
    abstract fun openRecoverWallet(fragment: Fragment, name: String?)
    abstract fun openMain(fragment: Fragment)
    abstract fun openSendConfirm(fragment: Fragment, fromaddress: String, toaddress:String, amount: Double, maxAmount: Boolean)
    abstract fun openSendSuccess(fragment: Fragment, transactionId: String)
    abstract fun openVpnExits(fragment: Fragment)
    abstract fun openChangePassword(fragment: Fragment)
    abstract fun openChangePin(fragment: Fragment)
    abstract fun openSendTransaction(fragment: Fragment, fromaddress: String)
    abstract fun openEnterWallet(fragment: Fragment)
    abstract fun openStart(fragment: Fragment)
}
