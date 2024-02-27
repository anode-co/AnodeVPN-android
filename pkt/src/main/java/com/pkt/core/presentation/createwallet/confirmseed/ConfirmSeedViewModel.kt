package com.pkt.core.presentation.createwallet.confirmseed

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.GeneralRepository
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfirmSeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val generalRepository: GeneralRepository,
) : StateViewModel<ConfirmSeedState>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private val pin: String = savedStateHandle["pin"] ?: throw IllegalArgumentException("pin required")
    private val seed: String = savedStateHandle["seed"] ?: throw IllegalArgumentException("seed required")
    private val name: String? = savedStateHandle["name"]

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
                //Launch pld
                generalRepository.launchPLD()
                invokeLoadingAction {
                    walletRepository.unlockWallet(password)
                        .onSuccess {
                            Timber.d("ConfirmSeedViewModel | Wallet created successfully")
                            sendNavigation(ConfirmSeedNavigation.ToCongratulations)
                        }
                        .onFailure {
                            Timber.e(it, "ConfirmSeedViewModel | Wallet creation failed")
                            sendError(it)
                        }
                }
            }
        }
    }
}
