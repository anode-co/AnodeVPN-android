package com.pkt.core.presentation.main.settings.walletinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pkt.domain.repository.WalletRepository
import com.pkt.core.R
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.GeneralRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generalRepository: GeneralRepository,
    private val walletRepository: WalletRepository,
) : StateViewModel<WalletInfoState>() {

    private var address: String = ""

    private val _timerUiState: MutableStateFlow<Int?> by lazy { MutableStateFlow(null) }
    val timerUiState: Flow<Int?> by lazy { _timerUiState }

    private var timerJob: Job? = null

    init {
        invokeLoadingAction {
            runCatching {
                walletRepository.getWalletAddress().getOrThrow()
            }.onSuccess {
                address = it
            }.onFailure {

            }
            runCatching {
                val balance = walletRepository.getWalletBalance(address).getOrThrow()
                val walletInfo = walletRepository.getWalletInfo().getOrThrow()
                if (walletInfo.wallet == null) {
                    //Wallet is locked
                    sendNavigation(AppNavigation.OpenEnterWallet)
                }
                Pair(balance, walletInfo)
            }.onSuccess { (balance, walletInfo) ->
                val wallet = walletInfo.wallet
                val neutrino = walletInfo.neutrino
                sendState {
                    copy(
                        items = mutableListOf<DisplayableItem>().apply {
                            addAll(
                                listOf(
                                    KeyValueVerticalItem(R.string.address, address),
                                    KeyValueVerticalItem(R.string.balance, listOf(balance), ValueFormatter.BALANCE),
                                    KeyValueVerticalItem(
                                        R.string.wallet_sync,
                                        listOf(wallet!!.currentHeight, wallet.currentBlockTimestamp),
                                        ValueFormatter.SYNC
                                    ),
                                    KeyValueVerticalItem(
                                        R.string.neutrino_sync,
                                        listOf(neutrino.height, neutrino.blockTimestamp),
                                        ValueFormatter.SYNC
                                    )
                                )
                            )

                            neutrino.peers.takeIf { it.isNotEmpty() }?.let { peers ->
                                add(ConnectedServersItem(peers.size))

                                addAll(
                                    peers.mapIndexed { index, peer ->
                                        ConnectedServerItem(peer.addr, peer.lastBlock, index < peers.size - 1)
                                    }
                                )
                            }
                        }
                    )
                }

                _timerUiState.update { 0 }

                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    while (isActive) {
                        delay(1_000L)
                        _timerUiState.update { (it ?: 0) + 1 }
                    }
                }
            }
        }
    }

    override fun createInitialState() = WalletInfoState()

    fun onDetailsClick() {
        sendNavigation(AppNavigation.OpenWebView("getinfo"))
    }

    fun onDebugLogsClick() {
        sendNavigation(AppNavigation.OpenWebView("pldlog"))
    }
}
