package com.pkt.core.presentation.enterwallet.choosewallet

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetChooseWalletBinding
import com.pkt.core.extensions.applyGradient
import com.pkt.core.presentation.common.BaseBottomSheet
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter

class ChooseWalletBottomSheet : BaseBottomSheet(R.layout.bottom_sheet_choose_wallet) {

    private val viewBinding by viewBinding(BottomSheetChooseWalletBinding::bind)

    private val adapter = AsyncListDifferAdapter(
        walletAdapterDelegate {
            setFragmentResult(REQUEST_KEY, bundleOf(WALLET_KEY to it.name))
            dismiss()
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            titleLabel.applyGradient()

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter
        }

        val wallets = arguments?.getStringArray(WALLETS_KEY)?.toList()!!
        val currentWallet = arguments?.getString(CURRENT_WALLET_KEY)!!

        adapter.items = wallets.mapIndexed { index, name ->
            WalletItem(
                name = name,
                isChecked = name == currentWallet,
                withDivider = index < wallets.size - 1
            )
        }
    }

    companion object {
        const val TAG = "choose_wallet_dialog"
        const val REQUEST_KEY = "choose_wallet_request"
        const val WALLET_KEY = "wallet"

        private const val WALLETS_KEY = "wallets"
        private const val CURRENT_WALLET_KEY = "current_wallet"

        fun newInstance(wallets: List<String>, currentWallet: String) = ChooseWalletBottomSheet().apply {
            arguments = bundleOf(
                WALLETS_KEY to wallets.toTypedArray(),
                CURRENT_WALLET_KEY to currentWallet
            )
        }
    }
}
