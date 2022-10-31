package com.pkt.core.presentation.main.wallet.qr

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetQrBinding
import com.pkt.core.extensions.applyGradient
import com.pkt.core.extensions.doOnClick
import com.pkt.core.presentation.common.state.StateBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QrBottomSheet : StateBottomSheet<QrState>(R.layout.bottom_sheet_qr) {

    private val viewBinding by viewBinding(BottomSheetQrBinding::bind)

    override val viewModel: QrViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            titleLabel.applyGradient()

            qrImage.clipToOutline = true

            saveButton.doOnClick {
                viewModel.onSaveClick()
            }

            doneButton.doOnClick {
                dismiss()
            }
        }
    }

    override fun handleState(state: QrState) {
        with(viewBinding) {
            addressValue.text = state.address
            qrImage.setImageBitmap(state.qr)
            qrProgressIndicator.isVisible = state.qr == null
        }
    }

    companion object {
        const val TAG = "qr_dialog"
    }
}
