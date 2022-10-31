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

    fun openCjdnsInfo(address: String) {
        sendNavigation(AppNavigation.OpenCjdnsInfo(address))
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

    fun openRecoverWallet() {
        sendNavigation(AppNavigation.OpenRecoverWallet)
    }

    fun openSendConfirm(address: String, amount: Double, maxAmount: Boolean) {
        sendNavigation(AppNavigation.OpenSendConfirm(address, amount, maxAmount))
    }
}
