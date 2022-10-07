package com.pkt.core.presentation.createwallet.creatingwallet

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.presentation.createwallet.CreateWalletViewModel
import com.pkt.core.R
import com.pkt.core.databinding.FragmentCreatingWalletBinding
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.state.CommonState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatingWalletFragment : StateFragment<CommonState.Empty>(R.layout.fragment_creating_wallet) {

    private val viewBinding by viewBinding(FragmentCreatingWalletBinding::bind)

    override val viewModel: CreatingWalletViewModel by viewModels()

    private val navigationViewModel: CreateWalletViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            viewModel.onBackPressed()
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
        super.handleEvent(event)

        when (event) {
            is CreatingWalletEvent.Back -> {
                navigationViewModel.navigateBack()
            }

            is CreatingWalletEvent.ToMain -> {
                navigationViewModel.toMain()
            }
        }
    }
}
