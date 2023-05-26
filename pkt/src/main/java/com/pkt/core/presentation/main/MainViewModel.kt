package com.pkt.core.presentation.main

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.main.wallet.transaction.details.TransactionDetailsExtra
import com.pkt.core.presentation.navigation.AppNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(

) : StateViewModel<CommonState.Empty>() {

    override fun createInitialState() = CommonState.Empty

    fun openWalletInfo() {
        sendNavigation(AppNavigation.OpenWalletInfo)
    }

    fun openCjdnsInfo() {
        sendNavigation(AppNavigation.OpenCjdnsInfo)
    }

    fun openVpnExits() {
        sendNavigation(AppNavigation.OpenVpnExits)
    }

    fun openChangePassword() {
        sendNavigation(AppNavigation.OpenChangePassword)
    }

    fun openChangePin() {
        sendNavigation(AppNavigation.OpenChangePin)
    }

    fun openCreateWallet(name: String) {
        sendNavigation(AppNavigation.OpenCreateWallet(name,CreateWalletMode.CREATE))
    }

    fun openRecoverWallet(name: String) {
        sendNavigation(AppNavigation.OpenCreateWallet(name,CreateWalletMode.RECOVER))
    }

    fun openSendTransaction(fromaddress: String) {
        sendNavigation(AppNavigation.OpenSendTransaction(fromaddress))
    }

    fun openSendConfirm(fromaddress: String,toaddress: String, amount: Double, maxAmount: Boolean) {
        sendNavigation(AppNavigation.OpenSendConfirm(fromaddress, toaddress, amount, maxAmount, false))
    }
    fun openSendConfirmPremiumVPN(fromaddress: String,toaddress: String, amount: Double) {
        sendNavigation(AppNavigation.OpenConfirmTransactionVPNPremium(fromaddress, toaddress, amount))
    }
    fun openEnterWallet() {
        sendNavigation(AppNavigation.OpenEnterWallet)
    }

    fun openStart() {
        sendNavigation(AppNavigation.OpenStart)
    }

    fun openTransactionDetails(extra: TransactionDetailsExtra) {
        sendNavigation(AppNavigation.OpenTransactionDetails(extra))
    }
}
