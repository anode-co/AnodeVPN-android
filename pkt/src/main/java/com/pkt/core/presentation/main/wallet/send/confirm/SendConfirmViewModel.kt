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

    private val fromaddress: String = savedStateHandle["fromaddress"] ?: error("fromaddress required")
    private val toaddress: String = savedStateHandle["toaddress"] ?: error("toaddress required")
    private val amount: Double = savedStateHandle.get<Float>("amount")?.toDouble() ?: error("amount required")
    private val maxAmount: Boolean = savedStateHandle["maxAmount"] ?: error("maxAmount required")

    init {
        invokeLoadingAction {
            runCatching {
                val isPinAvailable = walletRepository.isPinAvailable().getOrThrow()
                val amount = if (maxAmount) {
                    walletRepository.getTotalWalletBalance().getOrThrow()
                } else {
                    this@SendConfirmViewModel.amount
                }
                isPinAvailable to amount
            }.onSuccess { (isPinAvailable, amount) ->
                sendState {
                    copy(
                        address = this@SendConfirmViewModel.toaddress,
                        amount = amount,
                        isPinVisible = isPinAvailable,
                        confirmWithPasswordButtonVisible = isPinAvailable,
                        confirmWithPinButtonVisible = false
                    )
                }
                sendEvent(SendConfirmEvent.OpenKeyboard)
            }
        }
    }

    override fun createInitialState() = SendConfirmState()

    fun onPinDone(pin: String) {
        pin.takeIf { it.isNotBlank() } ?: return

        invokeAction {
            runCatching {
                val isPinCorrect = walletRepository.checkPin(pin).getOrThrow()
                if (isPinCorrect) {
                    walletRepository.sendCoins(listOf(fromaddress), currentState.amount.toLong(), currentState.address).getOrThrow()
                } else {
                    null
                }
            }.onSuccess { sendResponse ->
                sendResponse?.let {
                    sendNavigation(AppNavigation.NavigateBack)
                    sendNavigation(AppNavigation.OpenSendSuccess(it.txHash))
                } ?: run {
                    sendEvent(CommonEvent.Warning(R.string.error_pin_incorrect))
                    sendEvent(SendConfirmEvent.ClearInputs)
                    sendEvent(SendConfirmEvent.OpenKeyboard)
                }
            }.onFailure {
                sendError(it)
            }
        }
    }

    fun onPasswordDone(password: String) {
        password.takeIf { it.isNotBlank() } ?: return

        invokeAction {
            runCatching {
                val isPinCorrect = walletRepository.checkWalletPassphrase(password).getOrThrow()
                if (isPinCorrect) {
                    walletRepository.sendCoins(listOf(fromaddress), currentState.amount.toLong(), currentState.address).getOrThrow()
                } else {
                    null
                }
            }.onSuccess { sendResponse ->
                sendResponse?.let {
                    sendNavigation(AppNavigation.NavigateBack)
                    sendNavigation(AppNavigation.OpenSendSuccess(it.txHash))
                } ?: run {
                    sendEvent(CommonEvent.Warning(R.string.error_password_incorrect))
                    sendEvent(SendConfirmEvent.ClearInputs)
                    sendEvent(SendConfirmEvent.OpenKeyboard)
                }
            }.onFailure {
                sendError(it)
            }
        }
    }

    fun onConfirmWithPasswordClick() {
        sendState {
            copy(
                isPinVisible = false,
                confirmWithPasswordButtonVisible = false,
                confirmWithPinButtonVisible = true
            )
        }
        sendEvent(SendConfirmEvent.OpenKeyboard)
    }

    fun onConfirmWithPinClick() {
        sendState {
            copy(
                isPinVisible = true,
                confirmWithPasswordButtonVisible = true,
                confirmWithPinButtonVisible = false
            )
        }
        sendEvent(SendConfirmEvent.OpenKeyboard)
    }
}
