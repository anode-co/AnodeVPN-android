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
class WalletFragment : StateFragment<WalletState>(R.layout.fragment_wallet_core) {

    private val viewBinding by viewBinding(FragmentWalletBinding::bind)

    override val viewModel: WalletViewModel by viewModels()

    private val mainViewModel: MainViewModel by activityViewModels()

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
            val toaddress = bundle.getString(SendTransactionBottomSheet.KEY_TO_ADDRESS)!!
            val fromaddress = bundle.getString(SendTransactionBottomSheet.KEY_FROM_ADDRESS)!!
            val amount = bundle.getDouble(SendTransactionBottomSheet.KEY_AMOUNT)
            val maxAmount = bundle.getBoolean(SendTransactionBottomSheet.KEY_MAX_AMOUNT)
            mainViewModel.openSendConfirm(fromaddress, toaddress, amount, maxAmount)
        }

        with(viewBinding) {
            sendButton.setOnClickListener {
                mainViewModel.openSendTransaction(viewModel.walletAddress)
            }
            qrButton.setOnClickListener {
                showQrDialog()
            }
            shareButton.setOnClickListener {
                showShareAddress(viewModel.walletAddress)
            }
            selectPeriodButton.setOnClickListener {
                viewModel.onSelectPeriodClick()
            }

            transactionsLabel.applyGradient()

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter
        }
    }

    private fun showQrDialog() {
        QrBottomSheet().show(childFragmentManager, QrBottomSheet.TAG)
    }

    private fun showShareAddress(address: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, address)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    override fun handleState(state: WalletState) {
        with(viewBinding) {
            subtitleLabel.apply {
                val iconWarning = resources.getDrawable(R.drawable.warning, null)
                val dot = resources.getDrawable(R.drawable.ic_dot, null)
                when (state.syncState) {
                    WalletState.SyncState.DOWNLOADING -> {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
                        compoundDrawableTintList = ColorStateList.valueOf(
                            context.getColorByAttribute(R.attr.colorDownloading))
                    }
                    WalletState.SyncState.SCANNING -> {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
                        compoundDrawableTintList = ColorStateList.valueOf(
                            context.getColorByAttribute(R.attr.colorProgress))
                    }
                    WalletState.SyncState.SUCCESS -> {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
                        compoundDrawableTintList = ColorStateList.valueOf(
                            context.getColorByAttribute(R.attr.colorSuccess))
                    }
                    WalletState.SyncState.FAILED -> {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
                        compoundDrawableTintList = ColorStateList.valueOf(
                            context.getColorByAttribute(androidx.appcompat.R.attr.colorError))
                    }
                    WalletState.SyncState.WAITING -> {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(iconWarning, null, null, null)
                        compoundDrawableTintList = null
                    }
                }

                when (state.syncState) {
                    WalletState.SyncState.DOWNLOADING -> {
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.wallet_status_syncing_headers)} ${state.chainHeight}/${state.neutrinoTop}"
                    }
                    WalletState.SyncState.SCANNING -> {
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.wallet_status_syncing_transactions)} ${state.walletHeight}/${state.neutrinoTop}"
                    }
                    WalletState.SyncState.SUCCESS -> {
                        val diffSeconds = state.syncTimeDiff
                        var timeAgoText = ""
                        if (diffSeconds > 60) {
                            val minutes = diffSeconds / 60
                            if (minutes == (1).toLong()) {
                                timeAgoText = "$minutes "+getString(R.string.minute_ago)
                            } else {
                                timeAgoText = "$minutes "+getString(R.string.minutes_ago)
                            }
                        } else {
                            timeAgoText = "$diffSeconds "+getString(R.string.seconds_ago)
                        }
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.block)} ${state.chainHeight} - $timeAgoText"
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
                text = if (state.balancePkt.isEmpty()) {
                    "0.00"
                } else {
                    state.balancePkt.toLong().formatPkt()
                }
                applyGradient()
            }

            balanceUsdLabel.text = state.balanceUsd.formatUsd()

            addressValue.text = state.walletAddress

            adapter.items = state.items
        }

    }
}
