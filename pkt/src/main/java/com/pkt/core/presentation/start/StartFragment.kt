package com.pkt.core.presentation.start

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentStartBinding
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class StartFragment : StateFragment<StartState>(R.layout.fragment_start) {

    private val viewBinding by viewBinding(FragmentStartBinding::bind)

    override val viewModel: StartViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("StartFragment onViewCreated")
        with(viewBinding) {
            createButton.setOnClickListener {
                viewModel.onCreateClick()
            }
            recoverButton.setOnClickListener {
                viewModel.onRecoverClick()
            }
        }
        viewModel.fetchWallets()
    }

    override fun handleState(state: StartState) {
        viewBinding.contentGroup.isVisible = state.contentVisible
    }

    override fun onResume() {
        super.onResume()
        Timber.i("StartFragment onResume")
        viewModel.fetchWallets()
    }
}
