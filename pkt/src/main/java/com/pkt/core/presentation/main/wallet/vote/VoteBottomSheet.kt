package com.pkt.core.presentation.main.wallet.vote

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetSendTransactionBinding
import com.pkt.core.databinding.BottomSheetVoteBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import timber.log.Timber

@AndroidEntryPoint
class VoteBottomSheet : StateBottomSheet<VoteState>(R.layout.bottom_sheet_vote) {

    private val viewBinding by viewBinding(BottomSheetVoteBinding::bind)

    override val viewModel: SendTransactionViewModel by viewModels()

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

            updateAmountInputPadding()
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

    private fun updateAmountInputPadding() {
        with(viewBinding) {
            /*if (maxCheckbox.width > 0) {
                val newPadding = amountInput.paddingStart + maxCheckbox.width
                Timber.d("newPadding: $newPadding")

                amountInput.updatePaddingRelative(end = newPadding)
            } else {
                maxCheckbox.doOnNextLayout {
                    updateAmountInputPadding()
                }
            }*/
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
            }
//            maxLabel.isVisible = state.maxValueSelected
            voteButton.isEnabled = state.voteButtonEnabled
        }

    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            /*is VoteEvent.OpenKeyboard -> {
                viewBinding.amountInput.showKeyboardDelayed()
            }*/

            /*is VoteEvent.OpenSendConfirm -> {
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        KEY_FROM_ADDRESS to event.fromaddress,
                        KEY_TO_ADDRESS to event.toaddress,
                    )
                )
                dismiss()
            }*/

            /*is VoteEvent.AmountError -> {
                viewBinding.amountInputLayout.error = getString(event.errorResId)
                viewBinding.amountInput.showKeyboardDelayed()
            }*/
            
            is VoteEvent.AddressError -> {
                viewBinding.addressInputLayout.error = event.error
                viewBinding.addressInput.showKeyboardDelayed()
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
