package com.pkt.core.presentation.main.wallet.send.confirm

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SendConfirmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<SendConfirmState>() {

    private val address: String = savedStateHandle["address"] ?: error("address required")
    private val amount: Double = savedStateHandle.get<Float>("amount")?.toDouble() ?: error("amount required")
    private val maxAmount: Boolean = savedStateHandle["maxAmount"] ?: error("maxAmount required")

    init {
        if (maxAmount) {
            invokeLoadingAction {
                walletRepository.getTotalWalletBalance()
                    .onSuccess { amount ->
                        sendState { copy(address = this@SendConfirmViewModel.address, amount = amount) }
                        sendEvent(SendConfirmEvent.OpenKeyboard)
                    }
            }
        } else {
            sendState { copy(address = this@SendConfirmViewModel.address, amount = this@SendConfirmViewModel.amount) }
            sendEvent(SendConfirmEvent.OpenKeyboard)
        }
    }

    override fun createInitialState() = SendConfirmState()

    fun onPinDone(pin: String) {
        pin.takeIf { it.isNotBlank() } ?: return

        invokeAction {
            runCatching {
                val isPinCorrect = walletRepository.checkPin(pin).getOrThrow()
                if (isPinCorrect) {
                    walletRepository.send(currentState.address, currentState.amount).getOrThrow()
                } else {
                    null
                }
            }.onSuccess { sendResponse ->
                sendResponse?.let {
                    sendNavigation(AppNavigation.NavigateBack)
                    sendNavigation(AppNavigation.OpenSendSuccess(it.transactionId))
                } ?: run {
                    sendEvent(CommonEvent.Warning(R.string.error_pin_incorrect))
                    sendEvent(SendConfirmEvent.ClearPin)
                    sendEvent(SendConfirmEvent.OpenKeyboard)
                }
            }.onFailure {
                sendError(it)
            }
        }
    }
}
