package com.pkt.core.presentation.main.wallet.send.send

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetSendTransactionBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import timber.log.Timber

@AndroidEntryPoint
class SendTransactionBottomSheet : StateBottomSheet<SendTransactionState>(R.layout.bottom_sheet_send_transaction) {

    private val viewBinding by viewBinding(BottomSheetSendTransactionBinding::bind)

    override val viewModel: SendTransactionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            addressInputLayout.doOnTextChanged {
                viewModel.address = it
            }

            amountInputLayout.apply {
                doOnTextChanged {
                    viewModel.amount = it
                }

                clearFocusOnActionDone()
            }

            sendButton.doOnClick {
                viewModel.onSendClick()
            }

            updateAmountInputPadding()
        }
    }

    override fun onStart() {
        super.onStart()

        with(viewBinding) {
            when {
                addressInput.hasFocus() -> addressInput.showKeyboardDelayed()
                amountInput.hasFocus() -> amountInput.showKeyboardDelayed()
                else -> addressInput.showKeyboardDelayed()
            }
        }
    }

    private fun updateAmountInputPadding() {
        with(viewBinding) {
            if (maxCheckbox.width > 0) {
                val newPadding = amountInput.paddingStart + maxCheckbox.width
                Timber.d("newPadding: $newPadding")

                amountInput.updatePaddingRelative(end = newPadding)
            } else {
                maxCheckbox.doOnNextLayout {
                    updateAmountInputPadding()
                }
            }
        }
    }

    override fun handleState(state: SendTransactionState) {
        with(viewBinding) {
            maxCheckbox.apply {
                setOnCheckedChangeListener(null)

                maxCheckbox.isChecked = state.maxValueSelected

                doOnCheckChanged {
                    rootView?.findFocus()?.let {
                        it.clearFocus()
                        UIUtil.hideKeyboard(it.context, it)
                    }

                    viewModel.onMaxCheckChanged(isChecked)
                }
            }

            maxLabel.isVisible = state.maxValueSelected

            sendButton.isEnabled = state.sendButtonEnabled
        }
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is SendTransactionEvent.OpenKeyboard -> {
                viewBinding.amountInput.showKeyboardDelayed()
            }

            else -> super.handleEvent(event)
        }
    }
}
