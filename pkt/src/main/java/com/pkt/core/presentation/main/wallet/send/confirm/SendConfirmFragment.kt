package com.pkt.core.presentation.main.wallet.send.confirm

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentSendConfirmBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendConfirmFragment : StateFragment<SendConfirmState>(R.layout.fragment_send_confirm) {

    private val viewBinding by viewBinding(FragmentSendConfirmBinding::bind)

    override val viewModel: SendConfirmViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            pinInputLayout.doOnActionDone { inputLayout ->
                inputLayout.editText?.hideKeyboard()

                inputLayout.editText?.text?.let {
                    viewModel.onPinDone(it.toString())
                }
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
        }
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is SendConfirmEvent.OpenKeyboard -> {
                viewBinding.pinInput.postDelayed({
                    viewBinding.pinInput.showKeyboard()
                }, 100)
            }

            is SendConfirmEvent.ClearPin -> {
                viewBinding.pinInput.setText("")
            }

            else -> super.handleEvent(event)
        }
    }
}
