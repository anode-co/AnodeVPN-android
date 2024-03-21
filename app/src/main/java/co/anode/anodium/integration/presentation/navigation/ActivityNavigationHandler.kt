package co.anode.anodium.integration.presentation.navigation

import android.content.Intent
import androidx.fragment.app.Fragment
import co.anode.anodium.integration.presentation.*
import co.anode.anodium.integration.presentation.settings.CjdnsInfoActivity
import co.anode.anodium.integration.presentation.settings.WalletInfoActivity
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.main.wallet.transaction.details.TransactionDetailsExtra
import com.pkt.core.presentation.navigation.AppNavigationHandler
import com.pkt.domain.dto.Vote
import javax.inject.Inject

class ActivityNavigationHandler @Inject constructor() : AppNavigationHandler() {

    override fun navigateBack(fragment: Fragment) {
        fragment.requireActivity().finish()
    }

    override fun openCjdnsInfo(fragment: Fragment) {
        fragment.startActivity(CjdnsInfoActivity.getIntent(fragment.requireContext()))
    }

    override fun openWalletInfo(fragment: Fragment) {
        fragment.startActivity(WalletInfoActivity.getIntent(fragment.requireContext(), ""))
    }

    override fun openCreateWallet(fragment: Fragment, name: String?, mode: CreateWalletMode) {
        if (mode == CreateWalletMode.CREATE) {
            fragment.startActivity(CreateWalletActivity.getCreateWalletIntent(fragment.requireContext(), name))
        } else {
            fragment.startActivity(CreateWalletActivity.getRecoverWalletIntent(fragment.requireContext()))
        }
    }

    override fun openRecoverWallet(fragment: Fragment, name: String?) {
        fragment.startActivity(CreateWalletActivity.getRecoverWalletIntent(fragment.requireContext()))
    }

    override fun openMain(fragment: Fragment) {
        val walletActivity = Intent(fragment.requireContext(), WalletActivity::class.java)
        walletActivity.putExtra("WALLET_NAME", "wallet")
        walletActivity.putExtra("PKT_TO_USD", "0.1")
        fragment.startActivity(walletActivity)
    }

    override fun openSendConfirm(fragment: Fragment, fromaddress: String,toaddress: String, amount: Double, maxAmount: Boolean, isVote: Boolean, isVoteCandidate: Boolean) {
        TODO("Not yet implemented")
    }

    override fun openSendSuccess(fragment: Fragment, transactionId: String, premiumVpn: Boolean, address: String) {
        TODO("Not yet implemented")
    }

    override fun openVpnExits(fragment: Fragment) {
        fragment.startActivity(VPNExitsActivity.getIntent(fragment.requireContext()))
    }

    override fun openChangePassword(fragment: Fragment) {
        fragment.startActivity(ChangePasswordActivity.getIntent(fragment.requireContext()))
    }

    override fun openChangePin(fragment: Fragment) {
        fragment.startActivity(ChangePINActivity.getIntent(fragment.requireContext()))
    }

    override fun openChangePinFromChangePassword(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openSendTransaction(fragment: Fragment, fromaddress: String) {
        TODO("Not yet implemented")
    }
    override fun openVote(fragment: Fragment, fromaddress: String, isCandidate: Boolean) {
        TODO("Not yet implemented")
    }
    override fun openConfirmTransactionVPNPremium(fragment: Fragment, fromaddress: String, toAddress: String, amount: Double) {
        TODO("Not yet implemented")
    }


    override fun openEnterWallet(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openStart(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openTransactionDetails(fragment: Fragment, extra: TransactionDetailsExtra) {
        TODO("Not yet implemented")
    }

    override fun openVoteDetails(fragment: Fragment, vote: Vote) {
        TODO("Not yet implemented")
    }

    override fun openWebView(fragment: Fragment, html: String) {
        TODO("Not yet implemented")
    }
}
