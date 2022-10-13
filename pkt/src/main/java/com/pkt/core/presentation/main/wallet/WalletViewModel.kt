package com.pkt.core.presentation.main.wallet

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.ZonedDateTime
import com.pkt.core.extensions.*
import javax.inject.Inject
import kotlin.math.absoluteValue

@HiltViewModel
class WalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<WalletState>() {

    private val walletName: String = savedStateHandle["walletName"] ?: throw IllegalArgumentException("walletName is required")
    private val PKTtoUSD: String = savedStateHandle["PKTtoUSD"] ?: throw IllegalArgumentException("PKTtoUSD is required")


    init {
        invokeLoadingAction()
    }

    override fun createInitialState(): WalletState {
        return WalletState(
            syncState = WalletState.SyncState.SUCCESS,
            peersCount = 0,
            block = "",
            walletName = walletName,
            balancePkt = "",
            balanceUsd = "",
            walletAddress = "",
            items = listOf(
                FooterItem()
            )
        )
    }


    override fun createLoadingAction(): (suspend () -> Result<*>) = {
        runCatching {
            val info = walletRepository.getWalletInfo().getOrThrow()
            val addresses = walletRepository.getWalletBalances().getOrThrow()
            Pair(info, addresses)
        }.onSuccess { (info, addresses) ->
            val wallet = info.wallet
            val neutrino = info.neutrino
            val peerCount = neutrino.peers.size
            val neutrinoHeight = neutrino.height
            var neutrinoTop = neutrinoHeight
            for (i in 0 until peerCount) {
                if (neutrino.peers[i].lastBlock > neutrinoTop)
                    neutrinoTop = neutrino.peers[i].lastBlock
            }

            //Get the address with the highest balance
            var balanceString = ""
            var balance:Long = -1
            var address = ""
            for(i in 0 until addresses.addrs.size) {
                if (addresses.addrs[i].total > balance) {
                    balance = addresses.addrs[i].stotal.toLong()
                    address = addresses.addrs[i].address
                }
            }

            sendState {
                copy(
                    syncState = WalletState.SyncState.SUCCESS,
                    peersCount = peerCount,
                    block = "$neutrinoHeight/$neutrinoTop",
                    walletName = walletName,
                    balancePkt = balance.toString(),
                    balanceUsd = balance.toPKT().multiply(PKTtoUSD.toBigDecimal()).toString(),
                    walletAddress = address,
                    items = listOf()
                )
            }
            //Get transactions
            runCatching {
                walletRepository.getWalletTransactions().getOrThrow()
            }.onSuccess { t ->
                val transactions = t.transactions
                val items: MutableList<DisplayableItem> = mutableListOf()
                var prevDate: LocalDateTime = LocalDateTime.ofEpochSecond(0, 0, ZonedDateTime.now().offset)
                for(i in transactions.indices) {
                    //Add date
                    val date = LocalDateTime.ofEpochSecond(transactions[i].timeStamp.toLong(), 0, ZonedDateTime.now().offset)
                    if ((prevDate.dayOfMonth != date.dayOfMonth) ||
                        (prevDate.month != date.month) ||
                        (prevDate.year != date.year)) {
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
                        syncState = WalletState.SyncState.SUCCESS,
                        items = items,
                    )
                }
            }.onFailure {

            }
        }.onFailure {
            //TODO: identify error, check for unlock
            walletRepository.unlockWallet("password")
        }
    }

    fun onSendClick() {
        // TODO
    }

    fun onQrClick() {
        // TODO
    }

    fun onShareClick() {
        // TODO
    }

    fun onSelectPeriodClick() {
        // TODO
    }

    fun onRetryClick() {
        // TODO
    }
}
