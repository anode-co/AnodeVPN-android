package com.pkt.core.presentation.createwallet.confirmseed

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfirmSeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<ConfirmSeedState>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private val pin: String = savedStateHandle["pin"] ?: throw IllegalArgumentException("pin required")
    private val seed: String = savedStateHandle["seed"] ?: throw IllegalArgumentException("seed required")

    private val wordToCheck: String
        get() = seed.split(" ")[currentState.wordPosition]

    var word: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    override fun createInitialState() =
        ConfirmSeedState(wordPosition = (0 until seed.split(" ").size).shuffled().first())

    private fun invalidateNextButtonEnabled() {
        sendState {
            copy(
                nextButtonEnabled = word.isNotBlank()
            )
        }
    }

    fun onNextClick() {
        when {
            !wordToCheck.equals(word, ignoreCase = true) -> {
                sendEvent(ConfirmSeedEvent.ConfirmSeedError)
            }

            else -> {
                invokeAction {
                    walletRepository.createWallet(password, pin, seed, "wallet")
                        .onSuccess {
                            sendNavigation(ConfirmSeedNavigation.ToCongratulations)
                        }
                        .onFailure {
                            sendError(it)
                        }
                }
            }
        }
    }
}
