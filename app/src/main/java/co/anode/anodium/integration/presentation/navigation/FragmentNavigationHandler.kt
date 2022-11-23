package co.anode.anodium.integration.presentation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import co.anode.anodium.NavGraphDirections
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.navigation.AppNavigationHandler
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

    override fun openWalletInfo(fragment: Fragment, address: String) {
        navigate(fragment, NavGraphDirections.toWalletInfo(address))
    }

    override fun openCreateWallet(fragment: Fragment, name: String?, mode: CreateWalletMode) {
        navigate(fragment, NavGraphDirections.toCreateWallet(name, mode))
    }

    override fun openRecoverWallet(fragment: Fragment, name:String?) {
        navigate(fragment, NavGraphDirections.toRecoverWallet(name))
    }

    override fun openMain(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toMain())
    }

    override fun openSendTransaction(fragment: Fragment, fromAddress: String) {
        navigate(fragment, NavGraphDirections.toSendTransaction(fromAddress))
    }

    override fun openEnterWallet(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toEnterWallet())
    }

    override fun openStart(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toStart())
    }

    override fun openSendConfirm(fragment: Fragment, fromAddress:String, toAddress: String, amount: Double, maxAmount: Boolean) {
        navigate(fragment, NavGraphDirections.toSendConfirm(fromAddress, toAddress, amount.toFloat(), maxAmount))
    }

    override fun openSendSuccess(fragment: Fragment, transactionId: String) {
        navigate(fragment, NavGraphDirections.toSendSuccess(transactionId))
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

    private fun navigate(fragment: Fragment, navDirections: NavDirections) {
        fragment.findNavController()
            .takeIf { it.currentDestination?.getAction(navDirections.actionId) != null }
            ?.navigate(navDirections)
    }
}
