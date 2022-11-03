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
import com.pkt.core.databinding.FragmentWalletBinding
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
class WalletFragment : StateFragment<WalletState>(R.layout.fragment_wallet) {

    private val viewBinding by viewBinding(FragmentWalletBinding::bind)

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
                            WalletState.SyncState.DOWNLOADING -> R.attr.colorProgress
                            WalletState.SyncState.SCANNING -> R.attr.colorProgress
                            WalletState.SyncState.WAITING -> R.attr.colorProgress
                            WalletState.SyncState.SUCCESS -> R.attr.colorSuccess
                            WalletState.SyncState.FAILED -> androidx.appcompat.R.attr.colorError
                        }
                    )
                )
                when (state.syncState) {
                    WalletState.SyncState.SCANNING -> {
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.wallet_status_syncing_headers)} ${state.chainHeight}/${state.neutrinoTop}"
                    }
                    WalletState.SyncState.DOWNLOADING -> {
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.wallet_status_syncing_transactions)} ${state.walletHeight}/${state.neutrinoTop}"
                    }
                    WalletState.SyncState.SUCCESS -> {
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.block)} ${state.block}"
                    }
                    WalletState.SyncState.WAITING -> {
                        text = "${getString(R.string.wallet_status_syncing_waiting)}"
                    }
                    WalletState.SyncState.FAILED -> {
                        text = "${getString(R.string.wallet_status_disconnected)}"
                    }
                }
            }

            titleLabel.apply {
                text = state.walletName
                applyGradient()
            }

            balancePktLabel.apply {
                text = state.balancePkt.formatPkt(2)
                applyGradient()
            }

            balanceUsdLabel.text = state.balanceUsd.formatUsd()

            addressValue.text = state.walletAddress

            adapter.items = state.items
        }

    }
}
