package com.pkt.core.presentation.main.settings.renamewallet

import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RenameWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<RenameWalletState>() {

    var name: String = ""
        set(value) {
            field = value
            sendState { copy(saveButtonEnabled = value.isNotBlank()) }
        }

    init {
        viewModelScope.launch {
            walletRepository.getActiveWallet()
                .onSuccess {
                    sendEvent(RenameWalletEvent.FillWalletName(it))
                }
        }
    }

    override fun createInitialState() = RenameWalletState()

    fun onSaveClick() {
        invokeAction {
            walletRepository.renameWallet(name)
                .onSuccess { error ->
                    if (error == null) {
                        sendEvent(RenameWalletEvent.Dismiss)
                    } else {
                        sendEvent(RenameWalletEvent.ShowInputError(error))
                    }
                }.onFailure {
                    sendError(it)
                }
        }
    }
}
