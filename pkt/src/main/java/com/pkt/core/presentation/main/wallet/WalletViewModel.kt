package com.pkt.core.presentation.main.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pkt.core.R
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.ZonedDateTime
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.main.wallet.transaction.TransactionType
import com.pkt.core.presentation.main.wallet.transaction.details.TransactionDetailsExtra
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.dto.Transaction
import com.pkt.domain.repository.CjdnsRepository
import com.pkt.domain.repository.GeneralRepository
import com.pkt.domain.repository.VpnRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlin.math.absoluteValue

@HiltViewModel
class WalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val vpnRepository: VpnRepository,
    private val cjdnsRepository: CjdnsRepository,
    private val generalRepository: GeneralRepository,
) : StateViewModel<WalletState>() {

    private val refreshInterval: Long = 5000
    val walletName = walletRepository.getActiveWallet()
    var walletAddress = ""
    var balance: Long = 0
    private var PKTtoUSD = 0f
    private var firstTime = true
    private var transactions: List<Transaction> = emptyList()

    private var pollingJob: Job? = null

    init {
        invokeLoadingAction {
            val wallets = walletRepository.getAllWalletNames()
            //There are no wallets
            if (wallets.isEmpty()) {
                sendState { copy(syncState = WalletState.SyncState.NOTEXISTING) }
                return@invokeLoadingAction Result.failure<Unit>(IllegalStateException("No wallets"))
            }
            try {
                val peers = vpnRepository.getCjdnsPeers().getOrThrow()
                cjdnsRepository.addCjdnsPeers(peers)
            } catch (e: Exception) {
                if (e.message?.contains("Unable to resolve host") == true) {
                    sendState { copy(syncState = WalletState.SyncState.NOINTERNET)  }
                }
            }

            Result.success(true)
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
            balance = walletRepository.getTotalWalletBalance().getOrThrow()
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
                if (!generalRepository.hasInternetConnection()) {
                    walletSyncState = WalletState.SyncState.NOINTERNET
                }
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

            sendState {
                copy(
                    syncState = walletSyncState,
                    syncTimeDiff = syncTimeDiff,
                    chainHeight = neutrinoHeight,
                    neutrinoTop = neutrinoTop,
                    walletHeight = wallet!!.currentHeight,
                    peersCount = peerCount,
                    walletName = walletName,
                    balancePkt = balance.formatPkt(),
                    balanceUsd = balance.toPKT().multiply(PKTtoUSD.toBigDecimal()).toString(),
                    walletAddress = address
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
        val startTime = currentState.startDate?.div(1000) ?: 0L
        val endTime = currentState.endDate?.div(1000) ?: 0L
        //Get transactions
        runCatching {
            walletRepository.getWalletTransactions(coinbase = 1, reversed = false, skip = 0, limit = 20, startTime, endTime).getOrThrow()
        }.onSuccess { t ->
            if (transactions.size == t.transactions.size) return@onSuccess
            transactions = t.transactions
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
                var type = TransactionType.RECEIVE
                if (amount < 0)
                    type = TransactionType.SENT
                amount = amount.absoluteValue

                items.add(
                    TransactionItem(
                        id = i.toString(),
                        type = type,
                        time = date,
                        amountPkt = amount.formatPkt(),
                        amountUsd = amount.toPKT().multiply(PKTtoUSD.toBigDecimal()).toString(),
                        transactionId = transactions[i].txHash,
                        addresses = transactions[i].destAddresses,
                        blockNumber = transactions[i].blockHeight,
                    )
                )
            }
            items.add(FooterItem())
            sendState {
                copy(
                    items = items,
                )
            }
            delay(200)
            //After loading transactions for the 1st time scroll the list to top
            if (firstTime) {
                sendEvent(WalletEvent.ScrollToTop)
                firstTime = false
            }
        }.onFailure {
            Timber.e(it)
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
            items = listOf(FooterItem())
        )
    }

    fun onSelectPeriodClick() {
        sendEvent(WalletEvent.OpenDatePicker(currentState.startDate, currentState.endDate))
    }

    fun onSendClick() {
        if (balance > 1) {
            sendEvent(WalletEvent.OpenSendTransaction)
        } else {
            sendEvent(CommonEvent.Warning(R.string.error_insufficient_balance))
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
                if (PKTtoUSD == 0f) {
                    PKTtoUSD = walletRepository.getPktToUsd().getOrDefault(0f)
                }
                runCatching {
                    loadWalletInfo()
                }.onSuccess {
                    loadTransactions()
                }.onFailure {
                    sendState {
                        copy(syncState = WalletState.SyncState.LOCKED)
                    }
                }
                delay(refreshInterval)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    fun onPeriodChanged(startDate: Long?, endDate: Long?) {
        sendState { copy(startDate = startDate, endDate = endDate) }
        startPolling()
    }

    fun onDeletePeriodClick() {
        onPeriodChanged(null, null)
    }

    fun onAddressClick() {
        sendEvent(CommonEvent.CopyToBuffer(R.string.address, currentState.walletAddress))
        sendEvent(CommonEvent.Info(R.string.address_copied))
    }

    fun onTransactionClick(transaction: TransactionItem) {
        sendEvent(
            WalletEvent.OpenTransactionDetails(
                TransactionDetailsExtra(
                    transactionId = transaction.transactionId,
                    addresses = transaction.addresses,
                    blockNumber = transaction.blockNumber,
                    type = transaction.type,
                    time = transaction.time,
                    amountPkt = transaction.amountPkt,
                    amountUsd = transaction.amountUsd
                )
            )
        )
    }
}
