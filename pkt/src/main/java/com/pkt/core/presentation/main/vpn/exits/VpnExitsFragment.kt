package com.pkt.core.presentation.main.vpn.exits

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentVpnExitsBinding
import com.pkt.core.extensions.clearFocusOnActionSearch
import com.pkt.core.extensions.doOnTextChanged
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.main.common.consent.ConsentBottomSheet
import com.pkt.core.presentation.main.settings.SettingsEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VpnExitsFragment : StateFragment<VpnExitsState>(R.layout.fragment_vpn_exits) {

    private val viewBinding by viewBinding(FragmentVpnExitsBinding::bind)

    override val viewModel: VpnExitsViewModel by viewModels()

    private val adapter = AsyncListDifferAdapter(
        vpnExitAdapterDelegate {
            viewModel.onVpnExitItemClick(it)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(ShowVpnPremiumBottomSheet.REQUEST_KEY) { _, bundle ->
            viewModel.onShowVpnPremiumBottomSheetResult(bundle.getBoolean(ShowVpnPremiumBottomSheet.RESULT_KEY))
        }

        with(viewBinding) {
            searchInputLayout.apply {
                doOnTextChanged {
                    viewModel.query = it
                }

                clearFocusOnActionSearch()
            }

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter
        }
    }

    override fun handleState(state: VpnExitsState) {
        adapter.items = state.items
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)
        when (event) {
            is VpnExitsEvent.OpenVpnSelection -> {
                ShowVpnPremiumBottomSheet.newInstance(event.cost).show(parentFragmentManager, ShowVpnPremiumBottomSheet.TAG)
                //ShowVpnPremiumBottomSheet().show(parentFragmentManager, ShowVpnPremiumBottomSheet.TAG)
            }
        }
    }
}
