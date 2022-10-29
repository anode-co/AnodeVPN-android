package com.pkt.core.presentation.createwallet.seed

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<SeedState>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private val pin: String = savedStateHandle["pin"] ?: throw IllegalArgumentException("pin required")
    private val name: String? = savedStateHandle["name"]

    init {
        invokeLoadingAction {
            walletRepository.generateSeed(password, pin)
                .onSuccess {
                    sendState { copy(seed = it) }
                }
        }
    }

    override fun createInitialState() = SeedState()

    fun onCopyClick() {
        currentState.seed?.let {
            sendEvent(CommonEvent.CopyToBuffer(R.string.your_seed_phrase, it))
            sendEvent(CommonEvent.Info(R.string.seed_phrase_copied))
        }
    }

    fun onNextClick() {
        sendNavigation(SeedNavigation.ToConfirmSeed(password, pin, currentState.seed!!, name))
    }
}
