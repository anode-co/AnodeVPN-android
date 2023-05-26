package com.pkt.core.presentation.main.vpn.exits

import androidx.lifecycle.viewModelScope
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.util.CountryUtil
import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnState
import com.pkt.domain.repository.GeneralRepository
import com.pkt.domain.repository.VpnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VpnExitsViewModel @Inject constructor(
    private val vpnRepository: VpnRepository,
    private val generalRepository: GeneralRepository,
) : StateViewModel<VpnExitsState>() {

    private val _queryFlow: MutableStateFlow<String> by lazy { MutableStateFlow("") }
    private var selectedVpn: Vpn = Vpn("", "", "", true, false, false)

    var query: String = ""
        set(value) {
            field = value
            _queryFlow.tryEmit(value)
        }

    init {
        viewModelScope.launch {
            combine(
                vpnRepository.vpnListFlow,
                vpnRepository.currentVpnFlow,
                vpnRepository.vpnStateFlow,
                _queryFlow,
            ) { vpnList, currentVpn, vpnState, query ->
                sendState {
                    copy(
                        items = vpnList.map { vpn ->
                            VpnExitItem(
                                name = vpn.name,
                                countryFlag = CountryUtil.getCountryFlag(vpn.countryCode),
                                countryName = CountryUtil.getCountryName(vpn.countryCode),
                                countryCode = vpn.countryCode,
                                publicKey = vpn.publicKey,
                                isConnected = vpnState == VpnState.CONNECTED && vpn.name == currentVpn?.name,
                                isActive = vpn.isActive,
                                isPremium = vpn.isPremium,
                            )
                        }.filter {
                            query.isBlank()
                                    || it.name.startsWith(query, true)
                                    || it.countryName.startsWith(query, true)
                        }
                    )
                }
            }.collect()
        }

        invokeLoadingAction {
            vpnRepository.fetchVpnList(force = true, activeOnly = !generalRepository.getShowInactiveServers()).onFailure {
                if (it.message == "timeout") {
                    sendEvent(CommonEvent.Warning(R.string.connection_timeout))
                } else {
                    sendError(it)
                }
            }
        }
    }

    override fun createInitialState() = VpnExitsState()

    fun onVpnExitItemClick(item: VpnExitItem) {
        invokeAction {
            vpnRepository.disconnect()
        }
        selectedVpn = Vpn(name = item.name, countryCode = item.countryCode, publicKey = item.publicKey, true)
        if (item.isPremium) {
            sendEvent(VpnExitsEvent.OpenVpnSelection)
        } else {
            vpnRepository.connectFromExits(Vpn(name = item.name, countryCode = item.countryCode, publicKey = item.publicKey, true), false)
            navigateBack()
        }
    }

    fun onShowVpnPremiumBottomSheetResult(value: Boolean) {
        if (value) {
            if (selectedVpn.isPremium) {
                vpnRepository.connectFromExits(selectedVpn, true)
            } else {
                vpnRepository.connectFromExits(selectedVpn, true)
            }
        } else {
            vpnRepository.connectFromExits(selectedVpn, false)
        }
        navigateBack()
    }
}
