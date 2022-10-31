package com.pkt.core.presentation.createwallet.recoverwallet

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecoverWalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<RecoverWalletState>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private val pin: String = savedStateHandle["pin"] ?: throw IllegalArgumentException("pin required")

    var seed: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    var seedPassword: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    override fun createInitialState() = RecoverWalletState()

    private fun invalidateNextButtonEnabled() {
        sendState {
            copy(
                nextButtonEnabled = seed.isSeedValid() && seedPassword.isNotBlank()
            )
        }
    }

    private fun String.isSeedValid() = split(" ").map { it.trim() }.count { it.isNotBlank() } == SEED_LENGTH

    fun onNextClick() {
        invokeAction {
            walletRepository.recoverWallet(password, seed, seedPassword, "wallet")
                .onSuccess {
                    sendNavigation(RecoverWalletNavigation.ToCongratulations)
                }
                .onFailure {
                    sendError(it)
                }
        }
    }

    private companion object {
        const val SEED_LENGTH = 15
    }
}
