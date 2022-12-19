package com.pkt.core.presentation.main.wallet

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.pkt.core.R
import com.pkt.core.databinding.FragmentWalletCoreBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter
import com.pkt.core.presentation.common.adapter.EndlessRecyclerViewScrollListener
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.main.MainViewModel
import com.pkt.core.presentation.main.wallet.qr.QrBottomSheet
import com.pkt.core.presentation.main.wallet.send.send.SendTransactionBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.math.floor

@AndroidEntryPoint
class WalletFragment : StateFragment<WalletState>(R.layout.fragment_wallet_core) {

    private val viewBinding by viewBinding(FragmentWalletCoreBinding::bind)

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
        transactionAdapterDelegate {
            viewModel.onTransactionClick(it)
        }
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
        Timber.i("WalletFragment onViewCreated")
        with(viewBinding) {
            sendButton.setOnClickListener {
                viewModel.onSendClick()
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
            deletePeriodButton.setOnClickListener {
                viewModel.onDeletePeriodClick()
            }

            transactionsLabel.applyGradient()

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter
            recyclerView.addOnScrollListener(
                object : EndlessRecyclerViewScrollListener(recyclerView.layoutManager as LinearLayoutManager) {
                    override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                        viewModel.onLoadMore(page, totalItemsCount)
                    }
                })

            addressValue.doOnClick {
                viewModel.onAddressClick()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
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
                //val iconWarning = resources.getDrawable(R.drawable.warning, null)
                val dot = resources.getDrawable(R.drawable.ic_dot, null)
                setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
                when (state.syncState) {
                    WalletState.SyncState.NOTEXISTING -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorError))
                        text = "${getString(R.string.wallet_status_notexisting)}"
                        mainViewModel.openStart()
                    }
                    WalletState.SyncState.LOCKED -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorError))
                        text = "${getString(R.string.wallet_status_locked)}"
                        mainViewModel.openEnterWallet()
                    }
                    WalletState.SyncState.DOWNLOADING -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorProgress))
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.wallet_status_syncing_headers)} ${state.chainHeight}/${state.neutrinoTop}"
                    }
                    WalletState.SyncState.SCANNING -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorProgress))
                        text = "${
                            resources.getQuantityString(
                                R.plurals.peers,
                                state.peersCount,
                                state.peersCount
                            )
                        } | ${getString(R.string.wallet_status_syncing_transactions)} ${state.walletHeight}/${state.neutrinoTop}"
                    }
                    WalletState.SyncState.SUCCESS -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorSuccess))
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
                    WalletState.SyncState.FAILED -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(androidx.appcompat.R.attr.colorError))
                        text = "${getString(R.string.wallet_status_disconnected)}"
                    }
                    WalletState.SyncState.NOINTERNET -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(androidx.appcompat.R.attr.colorError))
                        text = "${getString(R.string.wallet_status_no_internet)}"
                    }
                    WalletState.SyncState.WAITING -> {
                        compoundDrawableTintList = ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorError))
                        text = "${getString(R.string.wallet_status_syncing_waiting)}"
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
                    state.balancePkt
                }
                applyGradient()
            }

            balanceUsdLabel.text = state.balanceUsd.formatUsd()
            if (state.walletAddress.length > 10) {
                addressValue.text = state.walletAddress.substring(0, 12) + "..." + state.walletAddress.substring(state.walletAddress.length - 8)
            } else {
                addressValue.text = state.walletAddress
            }
            adapter.items = state.items

            selectPeriodButton.text = if (state.startDate != null && state.endDate != null) {
                "${state.startDate.formatDateMMMDD()} - ${state.endDate.formatDateMMMDD()}"
            } else {
                getString(R.string.select_period)
            }
            deletePeriodButton.isVisible = state.startDate != null && state.endDate != null
        }

    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is WalletEvent.OpenDatePicker -> {
                val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                    .apply {
                        if (event.startDate != null && event.endDate != null) {
                            setSelection(androidx.core.util.Pair(event.startDate, event.endDate))
                        }
                    }
                    .build()

                datePicker.addOnPositiveButtonClickListener {
                    val startDate = it.first
                    val endDate = it.second
                    viewModel.onPeriodChanged(startDate, endDate)
                }
                datePicker.show(childFragmentManager, "date_picker")
            }
            is WalletEvent.OpenSendTransaction -> mainViewModel.openSendTransaction(viewModel.walletAddress)
            //is WalletEvent.ScrollToTop -> viewBinding.recyclerView.scrollToPosition(0)
            is WalletEvent.OpenTransactionDetails -> mainViewModel.openTransactionDetails(event.extra)
            else -> super.handleEvent(event)
        }
    }
}
