package com.pkt.core.presentation.main.wallet.vote.details

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class VoteDetails(
    val estimatedExpirationSec: String,
    val expirationBlock: Int,
    val isCandidate: Boolean,
    val voteBlock: Int,
    val voteFor: String,
    val voteTxid: String
): Parcelable