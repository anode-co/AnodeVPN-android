package com.pkt.core.presentation.main.wallet.vote

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetVoteBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.main.wallet.send.send.SendTransactionBottomSheet
import com.pkt.core.presentation.main.wallet.send.send.SendTransactionEvent
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import timber.log.Timber

@AndroidEntryPoint
class VoteBottomSheet : StateBottomSheet<VoteState>(R.layout.bottom_sheet_vote) {

    private val viewBinding by viewBinding(BottomSheetVoteBinding::bind)

    override val viewModel: VoteViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("VoteBottomSheet onViewCreated")
        with(viewBinding) {
            addressInputLayout.doOnTextChanged {
                viewModel.toaddress = it
            }

            voteButton.doOnClick {
                viewModel.onVoteClick()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        with(viewBinding) {
            when {
                addressInput.hasFocus() -> addressInput.showKeyboardDelayed()
                else -> addressInput.showKeyboardDelayed()
            }
        }
    }

    override fun handleState(state: VoteState) {
        with(viewBinding) {
            candidateVoteCheckbox.apply {
                setOnCheckedChangeListener(null)
                candidateVoteCheckbox.isChecked = state.candidateSelected

                doOnCheckChanged {
                    rootView?.findFocus()?.let {
                        it.clearFocus()
                        UIUtil.hideKeyboard(it.context, it)
                    }

                    viewModel.onCandidateCheckChanged(isChecked)
                }
            }
            withdrawVoteCheckbox.apply {
                setOnCheckedChangeListener(null)
                withdrawVoteCheckbox.isChecked = state.withdrawVoteSelected

                doOnCheckChanged {
                    rootView?.findFocus()?.let {
                        it.clearFocus()
                        UIUtil.hideKeyboard(it.context, it)
                    }
                    viewModel.onWithdrawVoteCheckChanged(isChecked)
                }
                if (state.withdrawVoteSelected) {
                    addressInput.hint = getString(R.string.vote_nobody)
                    addressInput.isEnabled = false
                    addressInput.text = Editable.Factory.getInstance().newEditable("")
                } else {
                    addressInput.hint = getString(R.string.vote_for_address)
                    addressInput.isEnabled = true
                }
            }
            voteButton.isEnabled = state.voteButtonEnabled
        }

    }

    override fun handleEvent(event: UiEvent) {
        when (event) {

            is VoteEvent.AddressError -> {
                viewBinding.addressInputLayout.error = event.error
                viewBinding.addressInput.showKeyboardDelayed()
            }

            is VoteEvent.OpenSendConfirm -> {
                setFragmentResult(
                    SendTransactionBottomSheet.REQUEST_KEY,
                    bundleOf(
                        SendTransactionBottomSheet.KEY_FROM_ADDRESS to event.fromaddress,
                        SendTransactionBottomSheet.KEY_TO_ADDRESS to event.toaddress,
                        SendTransactionBottomSheet.KEY_AMOUNT to event.amount,
                        SendTransactionBottomSheet.KEY_MAX_AMOUNT to event.maxAmount
                    )
                )
                dismiss()
            }

            else -> super.handleEvent(event)
        }
    }

    companion object {
        const val TAG = "vote_dialog"
        const val REQUEST_KEY = "vote_request"
        const val KEY_TO_ADDRESS = "toaddress"
        const val KEY_FROM_ADDRESS = "fromaddress"
    }
}
