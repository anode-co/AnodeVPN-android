package com.pkt.core.presentation.createwallet.seed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentSeedBinding
import com.pkt.core.extensions.doOnClick
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SeedFragment : StateFragment<SeedState>(R.layout.fragment_seed) {

    private val viewBinding by viewBinding(FragmentSeedBinding::bind)

    override val viewModel: SeedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            copyButton.setOnClickListener {
                viewModel.onCopyClick()
            }

            nextButton.doOnClick {
                viewModel.onNextClick()
            }
        }
    }

    override fun handleState(state: SeedState) {
        viewBinding.seedLabel.text = state.seed
    }
}
