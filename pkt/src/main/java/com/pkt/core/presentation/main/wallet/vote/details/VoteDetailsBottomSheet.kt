package com.pkt.core.presentation.main.wallet.vote.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.textview.MaterialTextView
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetTransactionDetailsBinding
import com.pkt.core.databinding.BottomSheetVoteDetailsBinding
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.formatDateLong
import com.pkt.core.extensions.formatTime
import com.pkt.core.extensions.formatUsd
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.main.wallet.transaction.TransactionType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VoteDetailsBottomSheet : StateBottomSheet<VoteDetailsState>(R.layout.bottom_sheet_vote_details) {

    private val viewBinding by viewBinding(BottomSheetVoteDetailsBinding::bind)

    override val viewModel: VoteDetailsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            copyButton.doOnClick {
                viewModel.onCopyClick()
            }

            viewButton.doOnClick {
                viewModel.onViewClick()
            }

            recipientAddressValue.doOnClick {
                viewModel.onAddressClick(recipientAddressValue.text.toString())
            }

        }
    }

    @SuppressLint("InflateParams")
    override fun handleState(state: VoteDetailsState) {
        with(viewBinding) {

            voteLabel.apply {
                if (state.vote.isCandidate) {
                    text = getString(R.string.vote_candidate)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        R.drawable.candidate_wave_green,
                        0,
                        0
                    )
                } else {
                    text = getString(R.string.vote)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        R.drawable.ic_vote_green,
                        0,
                        0
                    )
                }

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.green
                    )
                )

            }

            if (state.vote.voteFor.isEmpty()) {
                recipientAddressValue.text = getString(R.string.no_vote)
            } else if (state.vote.voteFor.startsWith("script:")) {
                recipientAddressValue.text = getString(R.string.vote_withdrawn)
            } else {
                recipientAddressValue.text = state.vote.voteFor
            }
            transactionIdValue.text = state.vote.voteTxid
            blockNumberValue.text = state.vote.voteBlock.toString()
            voteLabel.visibility = View.VISIBLE
        }
    }
}
