package com.pkt.core.presentation.main.wallet.send.send

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SendTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<SendTransactionState>() {

    private val fromaddress: String = savedStateHandle["fromaddress"]
        ?: throw IllegalArgumentException("fromAddress is required")
    var toaddress: String = ""
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
            toaddress = walletRepository.isPKTAddressValid(toaddress).getOrThrow()
        }.onSuccess {
            sendNavigation(
                AppNavigation.OpenSendConfirm(
                    fromaddress = fromaddress,
                    toaddress = toaddress,
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
                sendButtonEnabled = toaddress.isNotBlank() && (amount.toDoubleOrNull() != null || currentState.maxValueSelected)
            )
        }
    }
}
