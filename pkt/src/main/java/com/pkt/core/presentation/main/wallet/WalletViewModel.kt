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
import com.pkt.domain.dto.Transaction
import com.pkt.domain.dto.Vote
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
    var walletName = walletRepository.getActiveWallet()
    var walletAddress = ""
    var balance: Long = 0
    var vote: Vote = Vote(
        estimatedExpirationSec = "",
        expirationBlock = 0,
        isCandidate = false,
        voteBlock = 0,
        voteFor = "",
        voteTxid = ""
    )
    private var PKTtoUSD = 0f
    private var transactions: MutableList<Transaction> = mutableListOf()
    private var lastDateInTxnsList: LocalDateTime = LocalDateTime.ofEpochSecond(0, 0, ZonedDateTime.now().offset)

    private val txnsPerPage = 20
    private var pollingJob: Job? = null
    private var isLoadingMore = false

    init {
        invokeLoadingAction {
            val wallets = walletRepository.getAllWalletNames()
            //There are no wallets
            if (wallets.isEmpty()) {
                sendState { copy(syncState = WalletState.SyncState.NOTEXISTING) }
                return@invokeLoadingAction Result.failure<Unit>(IllegalStateException("No wallets"))
            } else {
                walletName = walletRepository.getActiveWallet()
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
                walletAddress = walletRepository.getWalletAddress().getOrNull() ?: ""
                //If no address, then create one
                if (walletAddress.isEmpty()) {
                    val response = walletRepository.createAddress().getOrThrow()
                    walletAddress = response.address
                    //Now try to resync the wallet
                    walletRepository.resyncWallet()
                }
            }
            balance = walletRepository.getTotalWalletBalance().getOrThrow()
            vote = walletRepository.getVote(walletAddress).getOrNull() ?: Vote(
                estimatedExpirationSec = "",
                expirationBlock = 0,
                isCandidate = false,
                voteBlock = 0,
                voteFor = "",
                voteTxid = ""
            )
            Pair(Triple(walletInfo, walletAddress, balance), vote)
        }.onSuccess { (w, v) ->
            val info = w.first
            val address = w.second
            val balance = w.third

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
                    walletName = walletName.removePrefix("wallet_"),
                    balancePkt = balance.formatPkt(),
                    balanceUsd = balance.toPKT().multiply(PKTtoUSD.toBigDecimal()).toString(),
                    walletAddress = address,
                    vote = v,
                )
            }
            return Result.success(true)
        }.onFailure {
            return Result.failure(it)
        }
        return Result.success(true)
    }

    private suspend fun loadTransactions(skip: Int = 0){
        val startTime = currentState.startDate?.div(1000) ?: 0L
        val endTime = currentState.endDate?.div(1000) ?: 0L

        //Get transactions
        runCatching {
            //Get one more txns to see if there are more
            walletRepository.getWalletTransactions(coinbase = 1, reversed = true, skip, txnsPerPage+1, startTime, endTime).getOrThrow()
        }.onSuccess { t ->
            //Do we have more transactions to load?
            val hasMoreTxns = t.transactions.size > txnsPerPage-1
            val tempList = mutableListOf<Transaction>()
            //reverse the list, drop last if > txnsPerPage
            if (hasMoreTxns && t.transactions.isNotEmpty()) {
                tempList.addAll(t.transactions.reversed().dropLast(1))
            } else {
                tempList.addAll(t.transactions.reversed())
            }

            //Remove duplicates
            if (transactions.isNotEmpty()) {
                for (j in 0 until transactions.size) {
                    for (i in tempList.indices) {
                        if (tempList[i].tx.txid == transactions[j].tx.txid) {
                            tempList.removeAt(i)
                            break
                        }
                    }
                }
            }
            // Remove vote transaction
            if (tempList.isNotEmpty()) {
                val tempListCopy = tempList.toList() // Create a copy of tempList
                for (i in tempListCopy.indices.reversed()) {
                    if ((tempListCopy[i].tx.vout.isNotEmpty() &&
                                tempListCopy[i].tx.vout[0].address.startsWith("script:")) ||
                        ((tempListCopy[i].tx.vout.size > 1) && // Check if tempListCopy[i].tx.vout[1] exists before accessing it
                                tempListCopy[i].tx.vout[1].address.startsWith("script:"))) {
                        tempList.remove(tempListCopy[i]) // Remove the element from the original list
                    }
                }
            }
            transactions.sortByDescending { it.time.toLong() }
            transactions.addAll(tempList)
            val items: MutableList<DisplayableItem> = mutableListOf()
            /*//Do not add loading, error and footer items from current list
            for (i in 0 until currentState.items.size) {
                if ((currentState.items[i].getItemId() != "LOADING_ITEM") &&
                    (currentState.items[i].getItemId() != "ERROR_ITEM") &&
                    (currentState.items[i].getItemId() != "FOOTER_ITEM")) {
                    items.add(currentState.items[i])
                }
            }*/
            sendState {
                copy(
                    items = listOf(),
                )
            }
            for (i in 0 until transactions.size) {
                //Add date
                val date = LocalDateTime.ofEpochSecond(transactions[i].time.toLong(), 0, ZonedDateTime.now().offset)
                if ((lastDateInTxnsList.dayOfMonth != date.dayOfMonth) ||
                    (lastDateInTxnsList.month != date.month) ||
                    (lastDateInTxnsList.year != date.year)
                ) {
                    items.add(DateItem(date))
                }
                lastDateInTxnsList = date
                if (transactions[i].tx.vout.isNotEmpty()) {
                    var amount = transactions[i].tx.vout[0].svalue.toLong()
                    var type = TransactionType.RECEIVE
                    if (amount < 0)
                        type = TransactionType.SENT
                    amount = amount.absoluteValue

                    val addresses = transactions[i].tx.vout.map { it.address }

                    items.add(
                        TransactionItem(
                            id = i.toString(),
                            type = type,
                            time = date,
                            amountPkt = amount.formatPkt(),
                            amountUsd = amount.toPKT().multiply(PKTtoUSD.toBigDecimal()).toString(),
                            transactionId = transactions[i].tx.txid,
                            addresses = addresses,
                            blockNumber = transactions[i].blockHeight,
                            confirmations = transactions[i].numConfirmations,
                        )
                    )
                } else {
                    Timber.e("No vout in transaction: ${transactions[i].tx.txid}")
                }
            }
            if (!hasMoreTxns) {
                items.add(FooterItem())
            }
            sendState {
                copy(
                    items = items,
                )
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
            walletName = walletName.removePrefix("wallet_"),
            balancePkt = "",
            balanceUsd = "",
            walletAddress = "",
            vote = Vote(
                estimatedExpirationSec = "",
                expirationBlock = 0,
                isCandidate = false,
                voteBlock = 0,
                voteFor = "",
                voteTxid = ""
            ),
            items = listOf(),
        )
    }

    fun onSelectPeriodClick() {
        sendEvent(WalletEvent.OpenDatePicker(currentState.startDate, currentState.endDate))
    }

    fun onSendClick() {
        if (balance > 1) {
            Timber.i("WalletViewModel onSendClick")
            sendEvent(WalletEvent.OpenSendTransaction)
        } else {
            Timber.i("WalletViewModel onSendClick. Balance is too low $balance")
            sendEvent(CommonEvent.Warning(R.string.error_insufficient_balance))
        }
    }

    fun onVoteClick() {
        if (balance > 1) {
            Timber.i("WalletViewModel onVoteClick")
            sendEvent(WalletEvent.OpenVote)
        } else {
            Timber.i("WalletViewModel onVoteClick. Balance is too low $balance")
            sendEvent(CommonEvent.Warning(R.string.error_insufficient_balance))
        }
    }

    fun onRetryClick() {

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
        //Add 23h:59m:59s to endDate
        val endDateWithTime = endDate?.plus(86399000)
        Timber.d("WalletViewModel onPeriodChanged: $startDate $endDateWithTime")
        sendState { copy(
            startDate = startDate,
            endDate = endDateWithTime,
            items = listOf())//Clear transactions when period changes
        }
        transactions.clear()
        startPolling()
    }

    fun onDeletePeriodClick() {
        onPeriodChanged(null, null)
    }

    fun onAddressClick() {
        sendEvent(CommonEvent.CopyToBuffer(R.string.address, currentState.walletAddress))
        sendEvent(CommonEvent.Info(R.string.address_copied))
    }

    fun onVoteAddressClick() {
        if (vote != null) {
            Timber.i("WalletViewModel onVoteClick")
            sendEvent(WalletEvent.OpenVoteDetails(vote!!))
        }
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

    fun onLoadMore(page: Int, totalItemsCount: Int) {
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            runCatching {
                sendState {
                    copy(items = currentState.items + LoadingItem())
                }
                //Load more transactions, skip the ones that are already loaded
                loadTransactions(transactions.size)
            }.onSuccess {
                isLoadingMore = false
            }.onFailure {
                isLoadingMore = false
                Timber.e(it)
                sendState {
                    copy(items = currentState.items + ErrorItem())
                }
            }
        }
    }
}
