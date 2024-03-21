package com.pkt.core.presentation.start

import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<StartState>() {

    init {
        fetchWallets()
    }

    fun fetchWallets() {
        viewModelScope.launch {
            var wallets = emptyList<String>()
            var attempts = 0
            while (wallets.isEmpty() && (attempts < 10)) {
                Timber.i("Wallets empty will try again attempt: $attempts")
                wallets = walletRepository.getAllWalletNames()
                delay(100)
                attempts++
            }
            if (wallets.isNotEmpty()) {
                Timber.i("Wallets found")
                sendNavigation(AppNavigation.OpenEnterWallet)
            } else {
                Timber.i("Wallets not found")
                sendState { copy(contentVisible = true) }
            }
        }
    }

    override fun createInitialState() = StartState()

    fun onCreateClick() {
        sendNavigation(AppNavigation.OpenCreateWallet(null,CreateWalletMode.CREATE))
    }

    fun onRecoverClick() {
        sendNavigation(AppNavigation.OpenCreateWallet(null,CreateWalletMode.RECOVER))
    }
}
