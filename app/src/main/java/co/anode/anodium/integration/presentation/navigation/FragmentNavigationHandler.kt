package co.anode.anodium.integration.presentation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import co.anode.anodium.NavGraphDirections
import com.pkt.core.presentation.navigation.AppNavigationHandler
import javax.inject.Inject

class FragmentNavigationHandler @Inject constructor() : AppNavigationHandler() {

    override fun navigateBack(fragment: Fragment) {
        if (!fragment.findNavController().popBackStack()) {
            fragment.requireActivity().finish()
        }
    }

    override fun openCjdnsInfo(fragment: Fragment, address: String) {
        navigate(fragment, NavGraphDirections.toCjdnsInfo(address))
    }

    override fun openWalletInfo(fragment: Fragment, address: String) {
        navigate(fragment, NavGraphDirections.toWalletInfo(address))
    }

    override fun openCreateWallet(fragment: Fragment, name: String?) {
        navigate(fragment, NavGraphDirections.toCreateWallet())
    }

    override fun openRecoverWallet(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toRecoverWallet())
    }

    override fun openMain(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toMain())
    }

    override fun openSendTransaction(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toSendTransaction())
    }

    override fun openSendConfirm(fragment: Fragment, address: String, amount: Double, maxAmount: Boolean) {
        navigate(fragment, NavGraphDirections.toSendConfirm(address, amount.toFloat(), maxAmount))
    }

    override fun openSendSuccess(fragment: Fragment, transactionId: String) {
        navigate(fragment, NavGraphDirections.toSendSuccess(transactionId))
    }

    override fun openVpnExits(fragment: Fragment) {
        navigate(fragment, NavGraphDirections.toVpnExits())
    }

    override fun openChangePassword(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openChangePin(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    private fun navigate(fragment: Fragment, navDirections: NavDirections) {
        fragment.findNavController()
            .takeIf { it.currentDestination?.getAction(navDirections.actionId) != null }
            ?.navigate(navDirections)
    }
}
