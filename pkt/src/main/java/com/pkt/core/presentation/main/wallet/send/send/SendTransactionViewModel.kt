package com.pkt.core.presentation.main.wallet.send.send

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SendTransactionViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<SendTransactionState>() {

    var address: String = ""
        set(value) {
            field = value
            invalidateSendButtonState()
        }

    var amount: String = ""
        set(value) {
            field = value
            invalidateSendButtonState()
        }

    override fun createInitialState() = SendTransactionState()

    fun onMaxCheckChanged(checked: Boolean) {
        sendState { copy(maxValueSelected = checked) }
        invalidateSendButtonState()

        if (!checked) {
            sendEvent(SendTransactionEvent.OpenKeyboard)
        }
    }

    fun onSendClick() {
        runCatching {
            address = walletRepository.isPKTAddressValid(address).getOrThrow()
        }.onSuccess {
            sendEvent(
                SendTransactionEvent.OpenSendConfirm(
                    address = address,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    maxAmount = currentState.maxValueSelected
                )
            )
        }.onFailure {
            //TODO: show error invalid address
            sendError(it)
        }
    }

    private fun invalidateSendButtonState() {
        sendState {
            copy(
                sendButtonEnabled = address.isNotBlank() && (amount.toDoubleOrNull() != null || currentState.maxValueSelected)
            )
        }
    }
}
