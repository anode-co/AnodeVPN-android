package com.pkt.core.presentation.main.wallet.send.send

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.extensions.toPKT
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
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
    private var balance = 0L

    init {
        invokeLoadingAction {
            runCatching {
                balance = walletRepository.getWalletBalance(fromaddress).getOrNull()!!
            }
        }
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
        var amountToSend = amount.toDoubleOrNull() ?: 0.0
        if (currentState.maxValueSelected) {
            Timber.d("SendTransactionViewModel onSendClick| Max value selected")
            amountToSend = 0.0
        } else if (amount.toDoubleOrNull() == 0.0) {
            Timber.d("SendTransactionViewModel onSendClick| Amount is 0")
            sendEvent(SendTransactionEvent.AmountError(R.string.error_send_transaction_amount))
            return
        }else if (amount.toDoubleOrNull()!! > balance.toPKT().toDouble()) {
            Timber.d("SendTransactionViewModel onSendClick| Amount $amount > balance ${balance.toPKT()}")
            sendEvent(SendTransactionEvent.AmountError(R.string.error_insufficient_balance))
            return
        }

        runCatching {
            toaddress = walletRepository.isPKTAddressValid(toaddress).getOrThrow()
        }.onSuccess {
            Timber.i("SendTransactionViewModel onSendClick| PKT address is valid")
            sendNavigation(
                AppNavigation.OpenSendConfirm(
                    fromaddress = fromaddress,
                    toaddress = toaddress,
                    amount = amountToSend ,
                    maxAmount = currentState.maxValueSelected,
                    premiumVpn = false
                )
            )
        }.onFailure {
            Timber.i("SendTransactionViewModel onSendClick| PKT address is invalid")
            sendEvent(SendTransactionEvent.AddressError(it.message))
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
