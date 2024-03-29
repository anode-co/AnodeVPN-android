package com.pkt.core.presentation.main.vpn

import android.annotation.SuppressLint
import android.net.VpnService
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.BuildConfig
import com.pkt.core.R
import com.pkt.core.databinding.FragmentVpnBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.main.MainViewModel
import com.pkt.core.presentation.main.common.consent.ConsentBottomSheet
import com.pkt.core.util.CountryUtil
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class VpnFragment : StateFragment<VpnState>(R.layout.fragment_vpn) {

    private val viewBinding by viewBinding(FragmentVpnBinding::bind)

    override val viewModel: VpnViewModel by viewModels()

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startVPNService()
        //If VPNExits is not allowed make button not clickable
        if (!BuildConfig.ALLOW_VPN_LIST) {
            viewBinding.vpnLayout.isEnabled = false
        }
        setFragmentResultListener(ConsentBottomSheet.REQUEST_KEY) { _, bundle ->
            //viewModel.onConsentResult(bundle.getBoolean(ConsentBottomSheet.RESULT_KEY))
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

    @SuppressLint("SetTextI18n")
    override fun handleState(state: VpnState) {
        with(viewBinding) {
            when(state.vpnState) {
                com.pkt.domain.dto.VpnState.CONNECT -> {
                    viewModel.onConnectionClick()
                }
                else -> {}
            }

            connectionStateLabel.apply {
                setText(
                    when (state.vpnState) {
                        com.pkt.domain.dto.VpnState.NO_INTERNET -> R.string.no_internet
                        com.pkt.domain.dto.VpnState.DISCONNECTED -> R.string.not_connected
                        com.pkt.domain.dto.VpnState.CONNECTING -> R.string.connecting
                        com.pkt.domain.dto.VpnState.CONNECT -> R.string.connecting
                        com.pkt.domain.dto.VpnState.GETTING_ROUTES -> R.string.getting_routes
                        com.pkt.domain.dto.VpnState.GOT_ROUTES -> R.string.got_routes
                        com.pkt.domain.dto.VpnState.CONNECTED -> R.string.connected
                        com.pkt.domain.dto.VpnState.CONNECTED_PREMIUM -> R.string.connected_premium
                    }
                )
                applyGradient()
            }

            connectionButton.setImageResource(
                when (state.vpnState) {
                    com.pkt.domain.dto.VpnState.DISCONNECTED -> R.drawable.ic_disconnected
                    com.pkt.domain.dto.VpnState.CONNECTING -> R.drawable.ic_connecting
                    com.pkt.domain.dto.VpnState.CONNECT -> R.drawable.ic_connecting
                    com.pkt.domain.dto.VpnState.GETTING_ROUTES -> R.drawable.ic_connecting
                    com.pkt.domain.dto.VpnState.GOT_ROUTES -> R.drawable.ic_connecting
                    com.pkt.domain.dto.VpnState.CONNECTED -> R.drawable.ic_connected_large
                    com.pkt.domain.dto.VpnState.NO_INTERNET -> R.drawable.ic_disconnected
                    com.pkt.domain.dto.VpnState.CONNECTED_PREMIUM -> R.drawable.ic_connected_large
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

            if (state.vpn?.requestPremium == true) {
                state.vpn.requestPremium = false
                state.vpn.let {
                    viewModel.requestPremiumAddress(it.publicKey)
                }
            }

            if (state.vpnState == com.pkt.domain.dto.VpnState.CONNECTED_PREMIUM) {
                val localDateTime = LocalDateTime.ofEpochSecond((state.premiumEndTime/1000), 0, ZoneOffset.systemDefault().rules.getOffset(Instant.now()) )
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                premiumConnectionTimeLabel.text = "${getString(R.string.premium_ends)} ${localDateTime.format(formatter)}"
            } else {
                premiumConnectionTimeLabel.text = ""
            }
        }
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is VpnEvent.OpenConsent -> {
                ConsentBottomSheet().show(parentFragmentManager, ConsentBottomSheet.TAG)
            }
            is VpnEvent.OpenConfirmTransactionVPNPremium -> {
                mainViewModel.openSendConfirmPremiumVPN(event.fromaddress, event.address, event.amount)
            }

            else -> super.handleEvent(event)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun startVPNService() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            viewModel.cjdnsInit()
        }
    }
}
