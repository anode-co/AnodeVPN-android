package com.pkt.core.presentation.main.settings.renamewallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetRenameWalletBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateBottomSheet
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RenameWalletBottomSheet : StateBottomSheet<RenameWalletState>(R.layout.bottom_sheet_rename_wallet) {

    private val viewBinding by viewBinding(BottomSheetRenameWalletBinding::bind)

    override val viewModel: RenameWalletViewModel by viewModels()

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

            saveButton.doOnClick {
                viewModel.onSaveClick()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        viewBinding.nameInput.showKeyboardDelayed()
    }

    override fun handleState(state: RenameWalletState) {
        viewBinding.saveButton.isEnabled = state.saveButtonEnabled
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is RenameWalletEvent.FillWalletName -> {
                viewBinding.nameInput.setText(event.name)
            }

            is RenameWalletEvent.ShowInputError -> {
                viewBinding.nameInputLayout.error = event.error
                viewBinding.nameInput.showKeyboardDelayed()
            }

            is RenameWalletEvent.Dismiss -> {
                dismiss()
            }

            else -> super.handleEvent(event)
        }
    }

    companion object {
        const val TAG = "rename_wallet_dialog"
    }
}
