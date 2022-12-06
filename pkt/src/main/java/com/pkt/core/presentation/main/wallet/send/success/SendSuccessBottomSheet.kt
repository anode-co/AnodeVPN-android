package com.pkt.core.presentation.main.wallet.send.success

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetSendSuccessBinding
import com.pkt.core.extensions.doOnClick
import com.pkt.core.presentation.common.state.StateBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SendSuccessBottomSheet : StateBottomSheet<SendSuccessState>(R.layout.bottom_sheet_send_success) {

    private val viewBinding by viewBinding(BottomSheetSendSuccessBinding::bind)

    override val viewModel: SendSuccessViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("SendSuccessBottomSheet onViewCreated")
        with(viewBinding) {
            copyButton.doOnClick {
                viewModel.onCopyClick()
            }

            viewButton.doOnClick {
                viewModel.onViewClick()
            }

            doneButton.doOnClick {
                dismiss()
            }
        }
    }

    override fun handleState(state: SendSuccessState) {
        with(viewBinding) {
            transactionIdValue.text = state.transactionId
        }
    }
}
