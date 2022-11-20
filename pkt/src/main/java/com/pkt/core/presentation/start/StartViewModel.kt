package com.pkt.core.presentation.start

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<StartState>() {

    init {
        invokeLoadingAction {
            runCatching {
                val wallets = walletRepository.getAllWalletNames()
                if (wallets.isEmpty()) {
                    wallets to null
                } else {
                    wallets to walletRepository.getWalletInfo().map { it.wallet }.getOrNull()
                }
            }.onSuccess { (wallets, wallet) ->
                if (wallets.isNotEmpty() && wallet == null) {
                    sendNavigation(AppNavigation.OpenEnterWallet)
                } else {
                    sendState { copy(contentVisible = true) }
                }
            }
        }
    }

    override fun createInitialState() = StartState()

    fun onCreateClick() {
        sendNavigation(AppNavigation.OpenCreateWallet())
    }

    fun onRecoverClick() {
        sendNavigation(AppNavigation.OpenRecoverWallet())
    }
}
