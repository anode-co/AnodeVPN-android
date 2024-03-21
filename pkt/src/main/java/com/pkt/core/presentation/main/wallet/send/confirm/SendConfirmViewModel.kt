package com.pkt.core.presentation.main.wallet.send.confirm

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.extensions.toPKT
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
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
    private val premiumVpn: Boolean? = savedStateHandle["premiumVpn"]
    private val isVote: Boolean? = savedStateHandle["isVote"]
    private val isVoteCandidate: Boolean? = savedStateHandle["isVoteCandidate"]

    init {
        invokeLoadingAction {
            if (isVote == true) {
                sendState { copy(isVote = true) }
            }
            runCatching {
                val isPinAvailable = walletRepository.isPinAvailable().getOrThrow()
                var amountToPass = amount
                if (maxAmount) {
                    amountToPass = walletRepository.getTotalWalletBalance().getOrThrow().toPKT().toDouble()
                }
                isPinAvailable to amountToPass
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
            }.onFailure {
                Timber.e(it, "SendConfirmViewModel init| Error")
            }
        }
    }

    override fun createInitialState() = SendConfirmState()

    fun onPinDone(pin: String) {
        pin.takeIf { it.isNotBlank() } ?: return
        invokeAction {
//            walletRepository.checkPin(pin).onSuccess {
            walletRepository.unlockWalletWithPIN(pin).onSuccess {
                Timber.d("SendConfirmViewModel onPinDone| Success")
                if ((premiumVpn != null) && (premiumVpn == true)){
                    walletRepository.createTransaction(listOf(fromaddress), currentState.amount, currentState.address).onSuccess {
                        Timber.i("SendConfirmViewModel createTransaction| Success")
                        sendNavigation(AppNavigation.NavigateBack)
                        sendNavigation(AppNavigation.OpenSendSuccess(it.transaction, premiumVpn, currentState.address))
                    }.onFailure {
                        Timber.e(it, "SendConfirmViewModel createTransaction| Error")
                        sendError(it)
                    }
                } else if (isVote == true) {
                    Timber.i("VoteViewModel onVoteClick| PKT address is valid")
                    walletRepository.sendVote(
                        fromAddress = fromaddress,
                        voteFor = toaddress,
                        isCandidate = isVoteCandidate ?: false
                    ).onSuccess {
                        Timber.i("VoteViewModel onVoteClick| Success")
                        sendEvent(CommonEvent.Info(R.string.vote_submitted))
                        navigateBack()
                    }.onFailure {
                        Timber.e(it, "VoteViewModel onVoteClick| Error")
                        sendError(it)
                    }
                } else {
                    walletRepository.sendCoins(listOf(fromaddress), currentState.amount, currentState.address).onSuccess {
                        Timber.i("SendConfirmViewModel sendCoins| Success")
                        sendNavigation(AppNavigation.NavigateBack)
                        sendNavigation(AppNavigation.OpenSendSuccess(it.txHash, false, ""))
                    }.onFailure {
                        Timber.e(it, "SendConfirmViewModel sendCoins| Error")
                        sendError(it)
                    }
                }
            }.onFailure {
                Timber.e(it, "SendConfirmViewModel onPinDone| Error")
                sendEvent(CommonEvent.Warning(R.string.error_pin_incorrect))
                sendEvent(SendConfirmEvent.ClearInputs)
                sendEvent(SendConfirmEvent.OpenKeyboard)
            }
        }
    }

    fun onPasswordDone(password: String) {
        password.takeIf { it.isNotBlank() } ?: return
        invokeAction {
//            walletRepository.checkWalletPassphrase(password).onSuccess {
            walletRepository.unlockWallet(password).onSuccess {
                Timber.d("SendConfirmViewModel onPasswordDone| Success")
                if ((premiumVpn != null) && (premiumVpn == true)){
                    walletRepository.createTransaction(listOf(fromaddress), currentState.amount, currentState.address).onSuccess {
                        Timber.i("SendConfirmViewModel createTransaction| Success")
                        sendNavigation(AppNavigation.NavigateBack)
                        sendNavigation(AppNavigation.OpenSendSuccess(it.transaction, premiumVpn, currentState.address))
                    }.onFailure {
                        Timber.e(it, "SendConfirmViewModel createTransaction| Error")
                        sendError(it)
                    }
                } else if (isVote == true) {
                    Timber.i("VoteViewModel onVoteClick| PKT address is valid")
                    walletRepository.sendVote(
                        fromAddress = fromaddress,
                        voteFor = toaddress,
                        isCandidate = isVoteCandidate ?: false
                    ).onSuccess {
                        Timber.i("VoteViewModel onVoteClick| Success")
                        sendEvent(CommonEvent.Info(R.string.vote_submitted))
                        navigateBack()
                    }.onFailure {
                        Timber.e(it, "VoteViewModel onVoteClick| Error")
                        sendError(it)
                    }
                } else {
                    walletRepository.sendCoins(listOf(fromaddress), currentState.amount, currentState.address).onSuccess {
                        Timber.i("SendConfirmViewModel sendCoins| Success")
                        sendNavigation(AppNavigation.NavigateBack)
                        sendNavigation(AppNavigation.OpenSendSuccess(it.txHash, false, ""))
                    }.onFailure {
                        Timber.e(it, "SendConfirmViewModel sendCoins| Error")
                        sendError(it)
                    }
                }
            }.onFailure {
                Timber.e(it, "SendConfirmViewModel onPasswordDone| Error")
                sendEvent(CommonEvent.Warning(R.string.error_password_incorrect))
                sendEvent(SendConfirmEvent.ClearInputs)
                sendEvent(SendConfirmEvent.OpenKeyboard)
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
