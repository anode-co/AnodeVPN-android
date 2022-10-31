package com.pkt.core.presentation.main.settings.deletewallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetDeleteWalletBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteWalletBottomSheet : StateBottomSheet<DeleteWalletState>(R.layout.bottom_sheet_delete_wallet) {

    private val viewBinding by viewBinding(BottomSheetDeleteWalletBinding::bind)

    override val viewModel: DeleteWalletViewModel by viewModels()

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

            checkbox.doOnCheckChanged {
                viewModel.checkboxChecked = it
            }

            deleteButton.doOnClick {
                viewModel.onDeleteClick()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        viewBinding.nameInput.showKeyboardDelayed()
    }

    override fun handleState(state: DeleteWalletState) {
        with(viewBinding) {
            description1Label.text = getString(R.string.delete_1, state.walletName)
            deleteButton.isEnabled = state.deleteButtonEnabled
        }
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is DeleteWalletEvent.FillWalletName -> {
                viewBinding.nameInput.setText(event.name)
            }

            is DeleteWalletEvent.ShowInputError -> {
                viewBinding.nameInputLayout.error = event.error
                viewBinding.nameInput.showKeyboardDelayed()
            }

            is DeleteWalletEvent.Dismiss -> {
                dismiss()
            }

            else -> super.handleEvent(event)
        }
    }

    companion object {
        const val TAG = "delete_wallet_dialog"
    }
}
