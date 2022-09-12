package com.pkt.core.presentation.main.settings.walletinfo

import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.UiState

data class WalletInfoState(
    val items: List<DisplayableItem> = emptyList(),
) : UiState
