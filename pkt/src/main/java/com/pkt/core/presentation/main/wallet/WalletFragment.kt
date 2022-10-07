package com.pkt.core.presentation.main.wallet

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletFragment : StateFragment<WalletState>(R.layout.fragment_wallet_core) {

    private val viewBinding by viewBinding(FragmentWalletCoreBinding::bind)

    override val viewModel: WalletViewModel by viewModels()

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

        with(viewBinding) {
            sendButton.setOnClickListener {
                viewModel.onSendClick()
            }
            qrButton.setOnClickListener {
                viewModel.onQrClick()
            }
            shareButton.setOnClickListener {
                viewModel.onShareClick()
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

    override fun handleState(state: WalletState) {
        with(viewBinding) {
            subtitleLabel.apply {
                compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(
                    when (state.syncState) {
                        WalletState.SyncState.PROGRESS -> R.attr.colorProgress
                        WalletState.SyncState.SUCCESS -> R.attr.colorSuccess
                        WalletState.SyncState.FAILED -> androidx.appcompat.R.attr.colorError
                    }))

                text = "${
                    resources.getQuantityString(R.plurals.peers,
                        state.peersCount,
                        state.peersCount)
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

            addressValue.text = state.walletAddress

            adapter.items = state.items
        }
    }
}
