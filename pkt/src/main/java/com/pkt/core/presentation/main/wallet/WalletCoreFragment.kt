package com.pkt.core.presentation.main.wallet

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentWalletCoreBinding
import com.pkt.core.extensions.applyGradient
import com.pkt.core.extensions.formatPkt
import com.pkt.core.extensions.formatUsd
import com.pkt.core.extensions.getColorByAttribute
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.main.MainViewModel
import com.pkt.core.presentation.main.wallet.qr.QrBottomSheet
import com.pkt.core.presentation.main.wallet.send.send.SendTransactionBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletCoreFragment : StateFragment<WalletState>(R.layout.fragment_wallet_core) {

    private val viewBinding by viewBinding(FragmentWalletCoreBinding::bind)

    override val viewModel: WalletViewModel by viewModels()

    private val mainViewModel: MainViewModel by activityViewModels()

    private var walletAddress: String = ""

    private val adapter = AsyncListDifferAdapter(
        loadingAdapterDelegate(),
        errorAdapterDelegate {
            viewModel.onRetryClick()
        },
        emptyAdapterDelegate(),
        footerAdapterDelegate(),
        dateAdapterDelegate(),
        transactionAdapterDelegate()
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(SendTransactionBottomSheet.REQUEST_KEY) { _, bundle ->
            val address = bundle.getString(SendTransactionBottomSheet.KEY_ADDRESS)!!
            val amount = bundle.getDouble(SendTransactionBottomSheet.KEY_AMOUNT)
            val maxAmount = bundle.getBoolean(SendTransactionBottomSheet.KEY_MAX_AMOUNT)
            mainViewModel.openSendConfirm(address, amount, maxAmount)
        }

        with(viewBinding) {
            sendButton.setOnClickListener {
                showSendTransactionDialog()
            }
            qrButton.setOnClickListener {
                showQrDialog()
            }
            shareButton.setOnClickListener {
                showShareAddress()
            }
            selectPeriodButton.setOnClickListener {
                viewModel.onSelectPeriodClick()
            }

            transactionsLabel.applyGradient()

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter

            //viewModel.startTimer()
        }
    }

    private fun showSendTransactionDialog() {
        SendTransactionBottomSheet().show(parentFragmentManager, SendTransactionBottomSheet.TAG)
    }

    private fun showQrDialog() {
        QrBottomSheet().show(childFragmentManager, QrBottomSheet.TAG)
    }

    private fun showShareAddress() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "$walletAddress")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    override fun handleState(state: WalletState) {
        with(viewBinding) {
            subtitleLabel.apply {
                compoundDrawableTintList = ColorStateList.valueOf(
                    context.getColorByAttribute(
                        when (state.syncState) {
                            WalletState.SyncState.PROGRESS -> R.attr.colorProgress
                            WalletState.SyncState.SUCCESS -> R.attr.colorSuccess
                            WalletState.SyncState.FAILED -> androidx.appcompat.R.attr.colorError
                        }
                    )
                )

                text = "${
                    resources.getQuantityString(
                        R.plurals.peers,
                        state.peersCount,
                        state.peersCount
                    )
                } / ${getString(R.string.block)} ${state.block}"
            }

            titleLabel.apply {
                text = state.walletName
                applyGradient()
            }

            balancePktLabel.apply {
                text = if (state.balancePkt.isEmpty()) {
                    "0.00"
                } else {
                    state.balancePkt.toLong().formatPkt()
                }
                applyGradient()
            }

            balanceUsdLabel.text = state.balanceUsd.formatUsd()
            walletAddress = state.walletAddress
            addressValue.text = walletAddress

            adapter.items = state.items
        }
    }
}
