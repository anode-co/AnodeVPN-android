package com.pkt.core.presentation.main.wallet.send.success

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.domain.repository.GeneralRepository
import com.pkt.domain.repository.VpnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SendSuccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vpnRepository: VpnRepository,
    private val generalRepository: GeneralRepository,
) : StateViewModel<SendSuccessState>() {

    private val transactionId: String = savedStateHandle["transactionId"] ?: error("transactionId required")
    private val address: String = savedStateHandle["address"] ?: ""
    private val isPremiumVpn: Boolean = savedStateHandle["premiumVpn"] ?: false
    override fun createInitialState() = SendSuccessState(transactionId = this.transactionId)

    fun onCopyClick() {
        sendEvent(CommonEvent.CopyToBuffer(R.string.transaction_id, transactionId))
        sendEvent(CommonEvent.Info(R.string.transaction_id_copied))
    }

    fun onViewClick() {
        sendEvent(CommonEvent.WebUrl("https://explorer.pkt.cash/tx/$transactionId"))
    }

    fun isPremiumVpn(): Boolean {
        return isPremiumVpn
    }

    fun requestPremium() {
        viewModelScope.launch {
            runCatching {
                vpnRepository.requestPremium(transactionId, address)
            }.onSuccess {
                if (it.isSuccess) {
                    sendEvent(CommonEvent.Info(R.string.premium_vpn_success))
                } else {
                    sendEvent(CommonEvent.Warning(R.string.premium_vpn_error))
                }
            }.onFailure {
                sendEvent(CommonEvent.Warning(R.string.premium_vpn_error))
            }
            navigateBack()
        }
    }
}
