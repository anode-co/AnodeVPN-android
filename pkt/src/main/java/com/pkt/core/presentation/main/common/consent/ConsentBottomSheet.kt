package com.pkt.core.presentation.main.common.consent

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetConsentBinding
import com.pkt.core.presentation.common.BaseBottomSheet
import com.pkt.domain.repository.GeneralRepository

class ConsentBottomSheet : BaseBottomSheet(R.layout.bottom_sheet_consent) {

    private val viewBinding by viewBinding(BottomSheetConsentBinding::bind)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            isCancelable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            checkboxOptOut.isChecked = arguments?.getBoolean("optOut") ?: false
            okButton.setOnClickListener {
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to checkboxOptOut.isChecked))
                dismiss()
            }
            cancelButton.setOnClickListener {
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "consent_dialog"
        const val REQUEST_KEY = "consent_request"
        const val RESULT_KEY = "result"
    }
}
