package co.anode.anodium.integration.presentation.navigation

import android.content.Intent
import androidx.fragment.app.Fragment
import co.anode.anodium.integration.presentation.CreateWalletActivity
import co.anode.anodium.integration.presentation.WalletActivity
import co.anode.anodium.integration.presentation.settings.CjdnsInfoActivity
import co.anode.anodium.integration.presentation.settings.WalletInfoActivity
import com.pkt.core.presentation.navigation.AppNavigationHandler
import javax.inject.Inject

class ActivityNavigationHandler @Inject constructor() : AppNavigationHandler() {

    override fun navigateBack(fragment: Fragment) {
        fragment.requireActivity().finish()
    }

    override fun openCjdnsInfo(fragment: Fragment, address: String) {
        fragment.startActivity(CjdnsInfoActivity.getIntent(fragment.requireContext(), address))
    }

    override fun openWalletInfo(fragment: Fragment, address: String) {
        fragment.startActivity(WalletInfoActivity.getIntent(fragment.requireContext(), address))
    }

    override fun openCreateWallet(fragment: Fragment, name: String?) {
        fragment.startActivity(CreateWalletActivity.getCreateWalletIntent(fragment.requireContext(), name))
    }

    override fun openRecoverWallet(fragment: Fragment) {
        fragment.startActivity(CreateWalletActivity.getRecoverWalletIntent(fragment.requireContext()))
    }

    override fun openMain(fragment: Fragment) {
        val walletActivity = Intent(fragment.requireContext(), WalletActivity::class.java)
        walletActivity.putExtra("WALLET_NAME", "wallet")
        walletActivity.putExtra("PKT_TO_USD", "0.1")
        fragment.startActivity(walletActivity)
    }

    override fun openSendTransaction(fragment: Fragment) {
        fragment
    }

    override fun openSendConfirm(fragment: Fragment, address: String, amount: Double, maxAmount: Boolean) {
        TODO("Not yet implemented")
    }

    override fun openSendSuccess(fragment: Fragment, transactionId: String) {
        TODO("Not yet implemented")
    }

    override fun openVpnExits(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openChangePassword(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openChangePin(fragment: Fragment) {
        TODO("Not yet implemented")
    }
}
