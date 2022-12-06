package com.pkt.core.presentation.main.wallet.transaction.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetTransactionDetailsBinding
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.formatDateLong
import com.pkt.core.extensions.formatTime
import com.pkt.core.extensions.formatUsd
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.main.wallet.transaction.TransactionType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionDetailsBottomSheet : StateBottomSheet<TransactionDetailsState>(R.layout.bottom_sheet_transaction_details) {

    private val viewBinding by viewBinding(BottomSheetTransactionDetailsBinding::bind)

    override val viewModel: TransactionDetailsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            copyButton.doOnClick {
                viewModel.onCopyClick()
            }

            seeMoreLessButton.doOnClick {
                viewModel.onSeeMoreLessClick()
            }
        }
    }

    @SuppressLint("InflateParams")
    override fun handleState(state: TransactionDetailsState) {
        with(viewBinding) {
            dateLabel.text = getString(R.string.transaction_date_time, state.extra.time.formatDateLong(), state.extra.time.formatTime())

            amountPktLabel.apply {
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    if (state.extra.type == TransactionType.SENT) R.drawable.ic_sent_90dp else R.drawable.ic_received_90dp,
                    0,
                    0
                )
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (state.extra.type == TransactionType.SENT) R.color.color1 else R.color.green
                    )
                )
                text = if (state.extra.type == TransactionType.SENT) "-%s".format(state.extra.amountPkt) else state.extra.amountPkt
            }

            amountUsdLabel.text = state.extra.amountUsd.formatUsd()
            senderAddressValue.text = state.extra.addresses.firstOrNull()
            transactionIdValue.text = state.extra.transactionId
            blockNumberValue.text = state.extra.blockNumber.toString()

            seeMoreLessButton.apply {
                isVisible = state.canSeeMore
                setText(if (state.moreVisible) R.string.see_less else R.string.see_more)
            }

            addressesLayout.isVisible = state.moreVisible

            addressesLayout.removeAllViews()
            state.extra.addresses.takeIf { it.size > 1 }?.subList(1, state.extra.addresses.size)?.forEach {
                val addressView = (layoutInflater.inflate(R.layout.view_address, null) as TextView).apply {
                    text = it
                }
                addressesLayout.addView(addressView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }
    }
}
