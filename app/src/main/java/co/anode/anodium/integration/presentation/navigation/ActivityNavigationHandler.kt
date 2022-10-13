package co.anode.anodium.integration.presentation.navigation

import androidx.fragment.app.Fragment
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

    override fun openCreateWallet(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openRecoverWallet(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun openMain(fragment: Fragment) {
        TODO("Not yet implemented")
    }
}
