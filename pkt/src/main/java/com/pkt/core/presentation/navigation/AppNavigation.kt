package com.pkt.core.presentation.navigation

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiNavigation
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.main.wallet.transaction.details.TransactionDetailsExtra
import com.pkt.domain.dto.Vote

sealed class AppNavigation : UiNavigation {

    object NavigateBack : AppNavigation()

    object OpenCjdnsInfo : AppNavigation()
    object OpenWalletInfo : AppNavigation()
    data class OpenCreateWallet(val name: String? = null, val mode: CreateWalletMode) : AppNavigation()
    data class OpenRecoverWallet(val name: String? = null) : AppNavigation()
    object OpenMain : AppNavigation()
    data class OpenSendTransaction(val fromAddress: String) : AppNavigation()
    data class OpenVote(val fromAddress: String, val isCandidate: Boolean) : AppNavigation()
    data class OpenConfirmTransactionVPNPremium(val fromAddress: String,val toAddress: String, val amount: Double) : AppNavigation()
    data class OpenSendConfirm(val fromaddress: String, val toaddress:String, val amount: Double, val maxAmount: Boolean, val premiumVpn: Boolean) : AppNavigation()
    data class OpenSendSuccess(val transactionId: String, val premiumVpn: Boolean, val address: String) : AppNavigation()
    data class OpenTransactionDetails(val extra: TransactionDetailsExtra) : AppNavigation()
    data class OpenVoteDetails(val vote: Vote) : AppNavigation()
    object OpenVpnExits : AppNavigation()
    object OpenChangePassword : AppNavigation()
    object OpenChangePin : AppNavigation()
    object OpenChangePinFromChangePassword : AppNavigation()
    object OpenEnterWallet : AppNavigation()
    object OpenStart : AppNavigation()
    data class OpenWebView(val html: String) : AppNavigation()
}

interface InternalNavigation : UiNavigation {
    val navDirections: NavDirections
}
