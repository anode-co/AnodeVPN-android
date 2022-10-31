package com.pkt.core.presentation.enterwallet

import com.pkt.core.R
import com.pkt.core.common.Constants
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.common.widget.PinKeyboardView
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EnterWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<EnterWalletState>() {

    var password: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    private var pinAttempts: Int = 0
        set(value) {
            field = value

            if (value >= Constants.MAX_PIN_ATTEMPTS) {
                sendState { copy(isPinAvailable = false) }
                sendEvent(EnterWalletEvent.ShowKeyboard)
            }
        }

    init {
        invokeLoadingAction {
            runCatching {
                val currentWallet = walletRepository.getActiveWallet().getOrThrow()
                val isPinAvailable = walletRepository.isPinAvailable().getOrThrow()

                val wallets = walletRepository.getAllWalletNames().getOrThrow()
                Triple(
                    currentWallet,
                    isPinAvailable,
                    wallets
                )
            }.onSuccess { (currentWallet, isPinAvailable, wallets) ->
                sendState {
                    copy(
                        wallets = wallets,
                        currentWallet = currentWallet,
                        isPinAvailable = isPinAvailable
                    )
                }
            }
        }
    }

    override fun createInitialState() = EnterWalletState()

    private fun invalidateNextButtonEnabled() {
        sendState {
            copy(loginButtonEnabled = password.isNotBlank())
        }
    }

    fun onKeyClick(key: PinKeyboardView.PinKey) {
        when (key) {
            PinKeyboardView.PinKey.KEY_1 -> sendState { copy(pin = "${pin}1") }
            PinKeyboardView.PinKey.KEY_2 -> sendState { copy(pin = "${pin}2") }
            PinKeyboardView.PinKey.KEY_3 -> sendState { copy(pin = "${pin}3") }
            PinKeyboardView.PinKey.KEY_4 -> sendState { copy(pin = "${pin}4") }
            PinKeyboardView.PinKey.KEY_5 -> sendState { copy(pin = "${pin}5") }
            PinKeyboardView.PinKey.KEY_6 -> sendState { copy(pin = "${pin}6") }
            PinKeyboardView.PinKey.KEY_7 -> sendState { copy(pin = "${pin}7") }
            PinKeyboardView.PinKey.KEY_8 -> sendState { copy(pin = "${pin}8") }
            PinKeyboardView.PinKey.KEY_9 -> sendState { copy(pin = "${pin}9") }
            PinKeyboardView.PinKey.KEY_0 -> sendState { copy(pin = "${pin}0") }
            PinKeyboardView.PinKey.KEY_LOG_OUT -> {
                // TODO
            }
            PinKeyboardView.PinKey.KEY_APPLY -> {
                checkPin()
            }
        }
    }

    fun onPinDeleteClick() {
        sendState { copy(pin = if (pin.isEmpty()) pin else pin.substring(0, pin.length - 1)) }
    }

    private fun checkPin() {
        invokeAction {
            walletRepository.unlockWalletWithPIN(currentState.pin)
                .onSuccess { isCorrect ->
                    if (isCorrect) {
                        sendNavigation(AppNavigation.OpenMain)
                    } else {
                        pinAttempts++
                        sendState { copy(pin = "") }
                        sendEvent(CommonEvent.Warning(R.string.error_pin_incorrect))
                    }
                }
                .onFailure {
                    pinAttempts++
                    sendState { copy(pin = "") }
                    sendEvent(CommonEvent.Warning(R.string.error_pin_incorrect))
                }
        }
    }

    fun onLoginClick() {
        invokeAction {
            walletRepository.unlockWallet(password)
                .onSuccess { isCorrect ->
                    if (isCorrect) {
                        sendNavigation(AppNavigation.OpenMain)
                    } else {
                        sendEvent(EnterWalletEvent.ClearPassword)
                        sendEvent(EnterWalletEvent.ShowKeyboard)
                        sendEvent(CommonEvent.Warning(R.string.error_password_incorrect))
                    }
                }
                .onFailure {
                    sendError(it)
                }
        }
    }

    fun onChooseWalletClick() {
        sendEvent(
            EnterWalletEvent.OpenChooseWallet(
                currentState.wallets,
                currentState.currentWallet
            )
        )
    }

    fun onWalletChanged(walletName: String?) {
        walletName
            ?.takeIf { it != currentState.currentWallet }
            ?.let { wallet ->
                invokeAction {
                    runCatching {
                        walletRepository.setActiveWallet(wallet)
                        walletRepository.isPinAvailable().getOrElse { false }
                    }.onSuccess { isPinAvailable ->
                        sendState { copy(currentWallet = wallet, pin = "", isPinAvailable = isPinAvailable) }
                        sendEvent(EnterWalletEvent.ClearPassword)
                        if (!isPinAvailable) {
                            sendEvent(EnterWalletEvent.ShowKeyboard)
                        }
                    }.onFailure {
                        sendError(it)
                    }
                }
            }
    }
}
