package com.pkt.core.presentation.main.settings.newwallet

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewWalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<NewWalletState>() {

    var name: String = ""
        set(value) {
            field = value
            sendState { copy(nextButtonEnabled = value.isNotBlank()) }
        }
    private val mode: CreateWalletMode = savedStateHandle["mode"] ?: CreateWalletMode.CREATE

    override fun createInitialState() = NewWalletState()

    fun onNextClick() {
        invokeAction {
            walletRepository.checkWalletName(name)
                .onSuccess {
                    sendEvent(NewWalletEvent.Success(name, mode))
                }.onFailure { error ->
                    error.message?.let { sendEvent(NewWalletEvent.ShowInputError(error.message!!)) }
                }
        }
    }
}
