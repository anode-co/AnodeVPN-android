package com.pkt.core.presentation.main.wallet.qr

import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<QrState>() {

    override fun createInitialState() = QrState()

    init {
        viewModelScope.launch {
            val address = walletRepository.getWalletAddress().getOrElse { "" }
            sendState { copy(address = address) }

            val qr = walletRepository.generateQr().getOrNull()
            sendState { copy(qr = qr) }
        }
    }

    fun onSaveClick() {
        // TODO("Not yet implemented")
    }
}
