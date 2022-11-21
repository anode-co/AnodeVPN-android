package com.pkt.core.presentation.createwallet.recoverwallet

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
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
            invalidateSeedInput()
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
                nextButtonEnabled = seed.seedLength() == SEED_LENGTH
            )
        }
    }

    private fun invalidateSeedInput() {
        sendState {
            copy(
                seedError = if (seed.seedLength() > SEED_LENGTH) R.string.error_seed_length else null
            )
        }
    }

    private fun String.seedLength() = split(" ").map { it.trim() }.count { it.isNotBlank() }

    fun onNextClick() {
        invokeAction {
            val walletName = walletRepository.getActiveWallet()
            walletRepository.recoverWallet(password, seed, seedPassword, walletName)
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
