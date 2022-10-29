package com.pkt.core.presentation.main.settings.newwallet

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetNewWalletBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.enterwallet.choosewallet.ChooseWalletBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewWalletBottomSheet : StateBottomSheet<NewWalletState>(R.layout.bottom_sheet_new_wallet) {

    private val viewBinding by viewBinding(BottomSheetNewWalletBinding::bind)

    override val viewModel: NewWalletViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            titleLabel.applyGradient()

            nameInputLayout.apply {
                doOnTextChanged {
                    viewModel.name = it
                }

                clearFocusOnActionDone()
            }

            nextButton.doOnClick {
                viewModel.onNextClick()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        viewBinding.nameInput.showKeyboardDelayed()
    }

    override fun handleState(state: NewWalletState) {
        viewBinding.nextButton.isEnabled = state.nextButtonEnabled
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is NewWalletEvent.ShowInputError -> {
                viewBinding.nameInputLayout.error = event.error
                viewBinding.nameInput.showKeyboardDelayed()
            }

            is NewWalletEvent.Success -> {
                setFragmentResult(REQUEST_KEY, bundleOf(WALLET_KEY to event.name))
                dismiss()
            }

            else -> super.handleEvent(event)
        }
    }

    companion object {
        const val TAG = "new_wallet_dialog"
        const val REQUEST_KEY = "new_wallet_request"
        const val WALLET_KEY = "wallet"
    }
}
