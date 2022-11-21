package com.pkt.core.presentation.main.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.ZonedDateTime
import com.pkt.core.extensions.*
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.CjdnsRepository
import com.pkt.domain.repository.VpnRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlin.math.absoluteValue

@HiltViewModel
class WalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val vpnRepository: VpnRepository,
    private val cjdnsRepository: CjdnsRepository,
) : StateViewModel<WalletState>() {

    val walletName = walletRepository.getActiveWallet()
    var walletAddress = ""
    var PKTtoUSD = 0f

    private var pollingJob: Job? = null

    init {
        invokeLoadingAction {
            val wallets = walletRepository.getAllWalletNames()
            //There are no wallets
            if (wallets.isEmpty()) {
                sendState { copy(syncState = WalletState.SyncState.NOTEXISTING) }
                return@invokeLoadingAction Result.failure<Unit>(IllegalStateException("No wallets"))
            }

            val peers = vpnRepository.getCjdnsPeers().getOrNull()
            if (peers != null) {
                cjdnsRepository.addCjdnsPeers(peers)
            } else {
                //TODO: no peering lines
            }
            runCatching {
                walletRepository.getPktToUsd().getOrNull()
            }.onSuccess { price ->
                PKTtoUSD = price!!
            }.onFailure {
                sendError(it)
                PKTtoUSD = 0f
            }
            runCatching {
                loadWalletInfo()
            }.onSuccess {
                loadTransactions()
            }.onFailure {
                //WalletInfo failed, try to unlock
                //sendNavigation(AppNavigation.OpenEnterWallet)
                sendState {
                    copy(syncState = WalletState.SyncState.LOCKED)
                }
            }
        }
    }

    private suspend fun loadWalletInfo(): Result<Boolean>{
        runCatching {
            val walletInfo = walletRepository.getWalletInfo().getOrThrow()
            if (walletInfo.wallet == null) {
                sendState {
                    copy(syncState = WalletState.SyncState.LOCKED)
                }
                throw IllegalStateException("Wallet is locked")
            } else {
                walletAddress = walletRepository.getWalletAddress().getOrThrow()
                //If no address, then create one
                if (walletAddress.isEmpty()) {
                    val response = walletRepository.createAddress().getOrThrow()
                    walletAddress = response.address
                }
            }
            val balance = walletRepository.getTotalWalletBalance().getOrThrow()
            Triple(walletInfo, walletAddress, balance)
        }.onSuccess { (info, address, balance) ->
            val wallet = info.wallet
            val neutrino = info.neutrino
            val peerCount = neutrino.peers.size
            val neutrinoHeight = neutrino.height
            var neutrinoTop = neutrinoHeight
            var syncTimeDiff: Long = 0
            var walletSyncState = WalletState.SyncState.FAILED
            for (i in 0 until peerCount) {
                if (neutrino.peers[i].lastBlock > neutrinoTop) {
                    neutrinoTop = neutrino.peers[i].lastBlock
                }
            }

            //Set wallet status
            if (peerCount == 0){
                walletSyncState = WalletState.SyncState.FAILED
            } else if (neutrinoHeight < neutrinoTop) {
                walletSyncState = WalletState.SyncState.DOWNLOADING
            } else if (wallet?.currentHeight!! < neutrinoHeight) {
                walletSyncState = WalletState.SyncState.SCANNING
            } else if (wallet.currentHeight == neutrinoHeight) {
                walletSyncState = WalletState.SyncState.SUCCESS
                val timestamp = neutrino.blockTimestamp
                var bTimestamp: Long = 0
                if (!timestamp.isNullOrEmpty()) {
                    bTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").parse(timestamp).time
                }
                syncTimeDiff = (System.currentTimeMillis() - bTimestamp) / 1000
                if (syncTimeDiff > 1440) {
                    walletSyncState = WalletState.SyncState.WAITING
                }
            }

            //DEBUG
            //walletSyncState = WalletState.SyncState.WAITING
            sendState {
                copy(
                    syncState = walletSyncState,
                    syncTimeDiff = syncTimeDiff,
                    chainHeight = neutrinoHeight,
                    neutrinoTop = neutrinoTop,
                    walletHeight = walletHeight,
                    peersCount = peerCount,
                    walletName = walletName,
                    balancePkt = balance.toLong().toString(),
                    balanceUsd = balance.toLong().toPKT().multiply(PKTtoUSD.toBigDecimal()).toString(),
                    walletAddress = address,
                    items = listOf()
                )
            }
            return Result.success(true)
        }.onFailure {
            //TODO: handle error
            return Result.failure(it)
        }
        return Result.success(true)
    }

    private suspend fun loadTransactions(){
        //Get transactions
        runCatching {
            walletRepository.getWalletTransactions().getOrThrow()
        }.onSuccess { t ->
            val transactions = t.transactions
            val items: MutableList<DisplayableItem> = mutableListOf()
            var prevDate: LocalDateTime = LocalDateTime.ofEpochSecond(0, 0, ZonedDateTime.now().offset)
            for (i in transactions.indices) {
                //Add date
                val date = LocalDateTime.ofEpochSecond(transactions[i].timeStamp.toLong(), 0, ZonedDateTime.now().offset)
                if ((prevDate.dayOfMonth != date.dayOfMonth) ||
                    (prevDate.month != date.month) ||
                    (prevDate.year != date.year)
                ) {
                    items.add(DateItem(date))
                }
                prevDate = date

                var amount = transactions[i].amount.toLong()
                var type = TransactionItem.Type.RECEIVE
                if (amount < 0)
                    type = TransactionItem.Type.SENT
                amount = amount.absoluteValue
                items.add(
                    TransactionItem(
                        id = i.toString(),
                        type = type,
                        time = date,
                        amountPkt = amount.toString(),
                        amountUsd = amount.toPKT().multiply(PKTtoUSD.toBigDecimal()).toString(),
                    )
                )
            }
            items.add(FooterItem())
            sendState {
                copy(
                    items = items,
                )
            }
        }.onFailure {
            //TODO: handle failure
        }
    }

    override fun createInitialState(): WalletState {
        return WalletState(
            syncState = WalletState.SyncState.WAITING,
            chainHeight = 0,
            neutrinoTop = 0,
            syncTimeDiff = 0,
            walletHeight = 0,
            peersCount = 0,
            walletName = walletName,
            balancePkt = "",
            balanceUsd = "",
            walletAddress = "",
            items = listOf(
                FooterItem()
            )
        )
    }

    fun onSelectPeriodClick() {
        onReload()
    }

    fun onReload() {
        viewModelScope.launch {
            runCatching {
                loadWalletInfo()
            }.onSuccess {
                loadTransactions()
            }.onFailure {
                //WalletInfo failed, try to unlock
                sendNavigation(AppNavigation.OpenEnterWallet)
                //TODO: check for error
                walletRepository.unlockWallet("password")
            }
        }
    }

    fun onRetryClick() {
        // TODO
    }

    fun onResume() {
        startPolling()
    }

    fun onPause() {
        stopPolling()
    }

    private fun startPolling() {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (true) {
                if (!isActive) return@launch

                // TODO: do some work here
                delay(1000L)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }
}
