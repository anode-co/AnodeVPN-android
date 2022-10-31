package com.pkt.core.presentation.createwallet.congratulations

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentCongratulationsBinding
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.createwallet.CreateWalletViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CongratulationsFragment : StateFragment<CommonState.Empty>(R.layout.fragment_congratulations) {

    private val viewBinding by viewBinding(FragmentCongratulationsBinding::bind)

    override val viewModel: CongratulationsViewModel by viewModels()

    private val navigationViewModel: CreateWalletViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            nextButton.setOnClickListener {
                viewModel.onNextClick()
            }
        }
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is CreatingWalletEvent.ToMain -> navigationViewModel.toMain()
            else -> super.handleEvent(event)
        }
    }
}
