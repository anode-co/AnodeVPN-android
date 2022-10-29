package com.pkt.core.presentation.createwallet.creatingwallet

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreatingWalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<CommonState.Empty>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private val pin: String = savedStateHandle["pin"] ?: throw IllegalArgumentException("pin required")
    private val seed: String = savedStateHandle["seed"] ?: throw IllegalArgumentException("seed required")

    init {
        invokeLoadingAction {
            walletRepository.createWallet(password, pin, seed, "wallet")
        }
    }

    override fun createInitialState() = CommonState.Empty

    fun onNextClick() {
        sendEvent(CreatingWalletEvent.ToMain)
    }

    fun onBackPressed() {
        if (currentLoadingState.isLoading
            || currentLoadingState.isRefreshing
            || currentLoadingState.loadingError != null
        ) {
            sendEvent(CreatingWalletEvent.Back)
        } else {
            onNextClick()
        }
    }
}
