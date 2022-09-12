package com.pkt.core.presentation.main.vpn

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentVpnBinding
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VpnFragment : StateFragment<VpnState>(R.layout.fragment_vpn) {

    private val viewBinding by viewBinding(FragmentVpnBinding::bind)

    override val viewModel: VpnViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            // TODO
        }
    }

    override fun handleState(state: VpnState) {
        // TODO
    }
}
