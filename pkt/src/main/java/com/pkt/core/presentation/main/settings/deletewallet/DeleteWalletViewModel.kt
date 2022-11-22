package com.pkt.core.presentation.main.settings.deletewallet

import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<DeleteWalletState>() {

    var name: String = ""
        set(value) {
            field = value
            invalidateButtonState()
        }

    var checkboxChecked: Boolean = false
        set(value) {
            field = value
            invalidateButtonState()
        }

    init {
        viewModelScope.launch {
            val activeWallet = walletRepository.getActiveWallet()
            sendState { copy(walletName = activeWallet) }
        }
    }

    override fun createInitialState() = DeleteWalletState()

    private fun invalidateButtonState() {
        sendState { copy(deleteButtonEnabled = name.isNotBlank() && checkboxChecked) }
    }

    fun onDeleteClick() {
        walletRepository.deleteWallet(name)
        //TODO: refresh wallets list in settings
    }
}
