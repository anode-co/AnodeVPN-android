package com.pkt.core.presentation.createwallet

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentCreateWalletBinding
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.state.CommonState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateWalletFragment : StateFragment<CommonState.Empty>(R.layout.fragment_create_wallet) {

    private val viewBinding by viewBinding(FragmentCreateWalletBinding::bind)

    override val viewModel: CreateWalletViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }

            val navHostFragment = childFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
            val navController = navHostFragment.navController

            navController.setGraph(R.navigation.nav_graph_create_wallet, arguments)

            navController.addOnDestinationChangedListener { _, destination, _ ->
                stepsView.currentStep =
                    when (destination.id) {
                        R.id.createPassword -> 0
                        R.id.confirmPassword -> 1
                        R.id.setPin -> 2
                        R.id.seed -> 3
                        R.id.confirmSeed -> 4
                        else -> 4
                    }

                toolbar.post { toolbar.isVisible = destination.id != R.id.congratulations }
            }

            stepsView.mode = arguments?.getSerializable("mode") as? CreateWalletMode ?: CreateWalletMode.CREATE
        }
    }

    companion object {
        private const val KEY_MODE = "mode"

        fun newCreateWalletInstance(mode: CreateWalletMode?) = CreateWalletFragment().apply {
            arguments = bundleOf(KEY_MODE to CreateWalletMode.CREATE)
        }

        fun newRecoverWalletInstance(mode: CreateWalletMode?) = CreateWalletFragment().apply {
            arguments = bundleOf(KEY_MODE to CreateWalletMode.RECOVER)
        }
    }
}
