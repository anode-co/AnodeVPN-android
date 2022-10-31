package com.pkt.core.presentation.main.wallet.qr

import android.graphics.Bitmap
import com.pkt.core.presentation.common.state.UiState

data class QrState(
    val address: String = "",
    val qr: Bitmap? = null,
) : UiState
