package co.anode.anodium.integration.presentation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import co.anode.anodium.NavGraphDirections
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.main.wallet.transaction.details.TransactionDetailsExtra
import com.pkt.core.presentation.main.wallet.vote.details.VoteDetails
import com.pkt.core.presentation.navigation.AppNavigationHandler
import com.pkt.domain.dto.Vote
import javax.inject.Inject

class FragmentNavigationHandler @Inject constructor() : AppNavigationHandler() {

    override fun navigateBack(fragment: Fragment) {
        if (!fragment.findNavController().popBackStack()) {
            fragment.requireActivity().finish()
        }
    }

    override fun openCjdnsInfo(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toCjdnsInfo())
    }

    override fun openWalletInfo(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toWalletInfo(""))
    }

    override fun openCreateWallet(fragment: Fragment, name: String?, mode: CreateWalletMode) {
        navigate(fragment, NavGraphDirections.toCreateWallet(name, mode))
    }

    override fun openRecoverWallet(fragment: Fragment, name:String?) {
        navigate(fragment, NavGraphDirections.toCreateWallet(name, CreateWalletMode.RECOVER))
    }

    override fun openMain(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toMain())
    }

    override fun openSendTransaction(fragment: Fragment, fromAddress: String) {
        navigate(fragment, NavGraphDirections.toSendTransaction(fromAddress))
    }

    override fun openVote(fragment: Fragment, fromAddress: String, isCandidate: Boolean) {
        navigate(fragment, NavGraphDirections.toVote(fromAddress, isCandidate))
    }

    override fun openConfirmTransactionVPNPremium(fragment: Fragment, fromAddress: String, toAddress: String, amount: Double) {
        navigate(fragment, NavGraphDirections.toSendConfirm(fromAddress, toAddress, amount.toFloat(), false, true, false, false))
    }

    override fun openEnterWallet(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toEnterWallet())
    }

    override fun openStart(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toStart())
    }

    override fun openSendConfirm(fragment: Fragment, fromAddress:String, toAddress: String, amount: Double, maxAmount: Boolean, isVote: Boolean,isVoteCandidate: Boolean) {
        navigate(fragment, NavGraphDirections.toSendConfirm(fromAddress, toAddress, amount.toFloat(), maxAmount, false, isVote, isVoteCandidate))
    }

    override fun openSendSuccess(fragment: Fragment, transactionId: String, premiumVpn: Boolean, address: String) {
        navigate(fragment, NavGraphDirections.toSendSuccess(transactionId, premiumVpn, address))
    }

    override fun openVpnExits(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toVpnExits())
    }

    override fun openChangePassword(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toChangePassword())
    }

    override fun openChangePin(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toChangePin())
    }

    override fun openChangePinFromChangePassword(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toChangePinFromChangePassword())
    }

    override fun openTransactionDetails(fragment: Fragment, extra: TransactionDetailsExtra) {
        navigate(fragment, NavGraphDirections.toTransactionDetails(extra))
    }

    override fun openVoteDetails(fragment: Fragment, vote: Vote) {
        val v = VoteDetails(
            vote.estimatedExpirationSec,
            vote.expirationBlock,
            vote.isCandidate,
            vote.voteBlock,
            vote.voteFor,
            vote.voteTxid
        )
        navigate(fragment, NavGraphDirections.toVoteDetails(v))
    }

    override fun openWebView(fragment: Fragment, html: String) {
        navigate(fragment, NavGraphDirections.toWebView(html))
    }

    private fun navigate(fragment: Fragment, navDirections: NavDirections) {
        fragment.findNavController()
            .takeIf { it.currentDestination?.getAction(navDirections.actionId) != null }
            ?.navigate(navDirections)
    }
}
