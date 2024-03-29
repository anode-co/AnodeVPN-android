package com.pkt.core.presentation.main.wallet.transaction.details

import android.os.Parcelable
import androidx.annotation.Keep
import com.pkt.core.presentation.main.wallet.transaction.TransactionType
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Keep
@Parcelize
data class TransactionDetailsExtra(
    val transactionId: String,
    val addresses: List<String>,
    val blockNumber: Int,
    val type: TransactionType,
    val time: LocalDateTime,
    val amountPkt: String,
    val amountUsd: String
) : Parcelable
