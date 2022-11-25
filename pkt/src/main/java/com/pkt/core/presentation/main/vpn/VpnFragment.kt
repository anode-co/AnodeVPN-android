package com.pkt.core.presentation.main.vpn

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentVpnBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.main.MainViewModel
import com.pkt.core.presentation.main.common.consent.ConsentBottomSheet
import com.pkt.core.util.CountryUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VpnFragment : StateFragment<VpnState>(R.layout.fragment_vpn) {

    private val viewBinding by viewBinding(FragmentVpnBinding::bind)

    override val viewModel: VpnViewModel by viewModels()

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(ConsentBottomSheet.REQUEST_KEY) { _, bundle ->
            viewModel.onConsentResult(bundle.getBoolean(ConsentBottomSheet.RESULT_KEY))
        }

        with(viewBinding) {
            connectionButton.setOnClickListener {
                viewModel.onConnectionClick()
            }
            vpnLayout.setOnClickListener {
                mainViewModel.openVpnExits()
            }
        }

        collectLatestRepeatOnLifecycle(viewModel.timerUiState) { seconds ->
            viewBinding.connectionTimeLabel.apply {
                text = seconds.formatSecondsLong()
                setTextColor(
                    if (seconds > 0) {
                        requireContext().getColorByAttribute(android.R.attr.colorPrimary)
                    } else {
                        ContextCompat.getColor(requireContext(), R.color.text1_50)
                    }
                )
            }
        }
    }

    override fun handleState(state: VpnState) {
        with(viewBinding) {
            connectionStateLabel.apply {
                setText(
                    when (state.vpnState) {
                        com.pkt.domain.dto.VpnState.DISCONNECTED -> R.string.not_connected
                        com.pkt.domain.dto.VpnState.CONNECTING -> R.string.connecting
                        com.pkt.domain.dto.VpnState.CONNECTED -> R.string.connected
                    }
                )
                applyGradient()
            }

            connectionButton.setImageResource(
                when (state.vpnState) {
                    com.pkt.domain.dto.VpnState.DISCONNECTED -> R.drawable.ic_disconnected
                    com.pkt.domain.dto.VpnState.CONNECTING -> R.drawable.ic_connecting
                    com.pkt.domain.dto.VpnState.CONNECTED -> R.drawable.ic_connected_large
                }
            )

            ipInfoLabel.text = StringBuilder().apply {
                state.ipV4?.let { ipv4 ->
                    append(getString(R.string.public_ipv4, ipv4))
                    append("\n")
                }
                state.ipV6?.let { ipv6 ->
                    append(getString(R.string.public_ipv6, ipv6))
                }
            }

            state.vpn?.let { vpn ->
                flagImage.text = CountryUtil.getCountryFlag(vpn.countryCode)
                nameLabel.text = vpn.name
                countryLabel.text = CountryUtil.getCountryName(vpn.countryCode)
            }
        }
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            VpnEvent.OpenConsent -> {
                ConsentBottomSheet().show(parentFragmentManager, ConsentBottomSheet.TAG)
            }

            else -> super.handleEvent(event)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
