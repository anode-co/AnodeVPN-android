package com.pkt.core.presentation.main

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.navigation.AppNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(

) : StateViewModel<CommonState.Empty>() {

    override fun createInitialState() = CommonState.Empty

    fun openWalletInfo(address: String) {
        sendNavigation(AppNavigation.OpenWalletInfo(address))
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
        sendNavigation(AppNavigation.OpenCreateWallet(name))
    }

    fun openRecoverWallet(name: String) {
        sendNavigation(AppNavigation.OpenRecoverWallet(name))
    }

    fun openSendTransaction(fromaddress: String) {
        sendNavigation(AppNavigation.OpenSendTransaction(fromaddress))
    }

    fun openSendConfirm(fromaddress: String,toaddress: String, amount: Double, maxAmount: Boolean) {
        sendNavigation(AppNavigation.OpenSendConfirm(fromaddress, toaddress, amount, maxAmount))
    }

    fun openEnterWallet() {
        sendNavigation(AppNavigation.OpenEnterWallet)
    }

    fun openStart() {
        sendNavigation(AppNavigation.OpenStart)
    }
}
