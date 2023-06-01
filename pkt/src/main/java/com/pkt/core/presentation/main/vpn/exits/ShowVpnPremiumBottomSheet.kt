package com.pkt.core.presentation.main.vpn.exits

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetChooseVpnPremiumBinding
import com.pkt.core.presentation.common.BaseBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowVpnPremiumBottomSheet: BaseBottomSheet(R.layout.bottom_sheet_choose_vpn_premium) {
    private val viewBinding by viewBinding(BottomSheetChooseVpnPremiumBinding::bind)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            isCancelable = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cost = arguments?.getInt(COST) ?: 0
        with(viewBinding) {
            premiumDescription.text = "${getString(R.string.premium_description)} $cost PKT"
            vpnFreeButton.setOnClickListener {
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to false))
                dismiss()
            }
            vpnPaidButton.setOnClickListener {
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to true))
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "showVpnPremium_dialog"
        const val REQUEST_KEY = "vpnPremium_request"
        const val RESULT_KEY = "result"
        const val COST = "vpnPremium_cost"

        fun newInstance(cost: Int): ShowVpnPremiumBottomSheet {
            return ShowVpnPremiumBottomSheet().apply {
                arguments = bundleOf(COST to cost)
            }
        }
    }
}