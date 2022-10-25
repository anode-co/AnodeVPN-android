package com.pkt.core.presentation.recoverwallet

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecoverWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<RecoverWalletState>() {

    var seed: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    var password: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    var seed_password: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    override fun createInitialState() = RecoverWalletState()

    private fun invalidateNextButtonEnabled() {
        sendState {
            copy(
                nextButtonEnabled = seed.isSeedValid() && password.isNotBlank()
            )
        }
    }

    private fun String.isSeedValid() = split(" ").map { it.trim() }.count { it.isNotBlank() } == SEED_LENGTH

    fun onNextClick() {
        invokeAction {
            walletRepository.recoverWallet(password, seed, seed_password, "wallet")
                .onSuccess {
                    sendNavigation(AppNavigation.OpenMain)
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
