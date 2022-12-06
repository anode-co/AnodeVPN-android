package com.pkt.core.presentation.main.wallet.send.confirm

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentSendConfirmBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SendConfirmFragment : StateFragment<SendConfirmState>(R.layout.fragment_send_confirm) {

    private val viewBinding by viewBinding(FragmentSendConfirmBinding::bind)

    override val viewModel: SendConfirmViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("SendConfirmFragment onViewCreated")
        with(viewBinding) {
            pinInputLayout.doOnActionDone { inputLayout ->
                inputLayout.editText?.hideKeyboard()

                inputLayout.editText?.text?.let {
                    viewModel.onPinDone(it.toString())
                }
            }

            passwordInputLayout.doOnActionDone { inputLayout ->
                inputLayout.editText?.hideKeyboard()

                inputLayout.editText?.text?.let {
                    viewModel.onPasswordDone(it.toString())
                }
            }

            confirmWithPasswordButton.doOnClick {
                viewModel.onConfirmWithPasswordClick()
            }
            confirmWithPinButton.doOnClick {
                viewModel.onConfirmWithPinClick()
            }
        }
    }

    override fun handleState(state: SendConfirmState) {
        with(viewBinding) {
            addressValue.text = state.address

            val formattedAmount = state.amount.formatPkt()
            val unit = formattedAmount.split(" ").lastOrNull().orEmpty()
            amountValue.text = formattedAmount.highlightText(
                requireContext().getColorByAttribute(android.R.attr.textColorSecondary),
                unit
            )

            pinInputLayout.isVisible = state.isPinVisible
            passwordInputLayout.isVisible = !state.isPinVisible
            confirmWithPasswordButton.isVisible = state.confirmWithPasswordButtonVisible
            confirmWithPinButton.isVisible = state.confirmWithPinButtonVisible

            toolbar.setTitle(
                if (state.isPinVisible) {
                    R.string.please_confirm_pin
                } else {
                    R.string.please_confirm_password
                }
            )
        }
    }

    override fun handleEvent(event: UiEvent) {
        with(viewBinding) {
            when (event) {
                is SendConfirmEvent.OpenKeyboard -> {
                    pinInputLayout.takeIf { it.isVisible }?.showKeyboardDelayed()
                    passwordInputLayout.takeIf { it.isVisible }?.showKeyboardDelayed()
                }

                is SendConfirmEvent.ClearInputs -> {
                    viewBinding.pinInput.setText("")
                    viewBinding.passwordInput.setText("")
                }

                else -> super.handleEvent(event)
            }
        }
    }
}
