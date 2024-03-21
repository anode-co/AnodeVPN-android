package com.pkt.core.presentation.main.settings.deletewallet

import androidx.lifecycle.viewModelScope
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.main.settings.renamewallet.RenameWalletEvent
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
            sendState { copy(
                walletName = walletRepository.getActiveWallet().removePrefix("wallet_"),
                deleteButtonEnabled = false
            ) }
        }
    }

    override fun createInitialState() = DeleteWalletState()

    private fun invalidateButtonState() {
        sendState { copy(deleteButtonEnabled = name.isNotBlank() && name == walletName && checkboxChecked) }
    }

    fun onDeleteClick() {
        viewModelScope.launch {
            walletRepository.deleteWallet(name)
            sendEvent(CommonEvent.Info(R.string.success))
            sendEvent(DeleteWalletEvent.Dismiss)
        }
    }
}
