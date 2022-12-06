package com.pkt.core.presentation.main.vpn.exits

import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.util.CountryUtil
import com.pkt.domain.dto.Vpn
import com.pkt.domain.dto.VpnState
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
) : StateViewModel<VpnExitsState>() {

    private val _queryFlow: MutableStateFlow<String> by lazy { MutableStateFlow("") }

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
            vpnRepository.fetchVpnList(force = true)
        }
    }

    override fun createInitialState() = VpnExitsState()

    fun onVpnExitItemClick(item: VpnExitItem) {
        invokeAction {
            vpnRepository.disconnect()
            vpnRepository.setCurrentVpn(Vpn(name = item.name, countryCode = item.countryCode, publicKey = item.publicKey))
                .onSuccess {
                    vpnRepository.connect(item.publicKey)
                    navigateBack()
                }
                .onFailure {
                    sendError(it)
                }
        }
    }
}
