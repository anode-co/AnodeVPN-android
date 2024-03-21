package com.pkt.core.presentation.main.settings.renamewallet

import androidx.lifecycle.viewModelScope
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
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
            val activeWallet = walletRepository.getActiveWallet().removePrefix("wallet_")
            sendEvent(RenameWalletEvent.FillWalletName(activeWallet))
        }
    }

    override fun createInitialState() = RenameWalletState()

    fun onSaveClick() {
        invokeAction {
            walletRepository.renameWallet(name)
                .onSuccess {
                    Timber.d("Wallet renamed to $name")
                    sendEvent(CommonEvent.Info(R.string.success))
                    sendEvent(RenameWalletEvent.Dismiss)
                }.onFailure {
                    Timber.e(it, "Error renaming wallet")
                    sendEvent(RenameWalletEvent.ShowInputError(it.localizedMessage))
                }
        }
    }
}
