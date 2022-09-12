package com.pkt.core.presentation.main.wallet

import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.Month
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(

) : StateViewModel<WalletState>() {

    init {
        // TODO
    }

    override fun createInitialState() = WalletState(
        syncState = WalletState.SyncState.SUCCESS,
        peersCount = 7,
        block = "1483428/1483429",
        walletName = "My Wallet 1",
        balancePkt = "98",
        balanceUsd = "1960.02",
        walletAddress = "pkdfvdsv1kliblh5kihi3nn24inkklk5353kkjbl",
        items = listOf(
            DateItem(LocalDateTime.of(2022, Month.JUNE, 23, 0, 0)),
            TransactionItem(
                id = "1",
                type = TransactionItem.Type.RECEIVE,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.SENT,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 10, 4),
                amountPkt = "10000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.SENT,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 8, 56),
                amountPkt = "1000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.RECEIVE,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 8, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),

            DateItem(LocalDateTime.of(2022, Month.JUNE, 21, 0, 0)),
            TransactionItem(
                id = "1",
                type = TransactionItem.Type.RECEIVE,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.SENT,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "10000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.SENT,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.RECEIVE,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),

            DateItem(LocalDateTime.of(2022, Month.JUNE, 16, 0, 0)),
            TransactionItem(
                id = "1",
                type = TransactionItem.Type.RECEIVE,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.SENT,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "10000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.SENT,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),
            TransactionItem(
                id = "2",
                type = TransactionItem.Type.RECEIVE,
                time = LocalDateTime.of(2022, Month.JUNE, 23, 12, 4),
                amountPkt = "1000",
                amountUsd = "20",
            ),

            FooterItem()
        )
    )

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
