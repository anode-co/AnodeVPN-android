package com.pkt.core.presentation.main.wallet.qr

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Point
import android.view.Display
import android.view.WindowManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QrViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<QrState>() {

    override fun createInitialState() = QrState()

    init {
        viewModelScope.launch {
            val address = walletRepository.getWalletAddress().getOrElse { "" }

            val qr = walletRepository.generateQr(address).getOrNull()
            sendState { copy(qr = qr) }
        }
    }

    fun onSaveClick() {
        // TODO("Not yet implemented")
    }
}
