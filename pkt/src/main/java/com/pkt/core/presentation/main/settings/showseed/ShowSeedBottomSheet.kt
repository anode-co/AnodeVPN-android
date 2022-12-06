package com.pkt.core.presentation.main.settings.showseed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.BottomSheetShowSeedBinding
import com.pkt.core.extensions.applyGradient
import com.pkt.core.presentation.common.state.StateBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowSeedBottomSheet : StateBottomSheet<ShowSeedState>(R.layout.bottom_sheet_show_seed) {

    private val viewBinding by viewBinding(BottomSheetShowSeedBinding::bind)

    override val viewModel: ShowSeedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            titleLabel.applyGradient()

            copyButton.setOnClickListener {
                viewModel.onCopyClick()
                dismiss()
            }
        }
    }

    override fun handleState(state: ShowSeedState) {
        viewBinding.seedLabel.text = state.seed
    }

    companion object {
        const val TAG = "show_seed_dialog"
    }
}
