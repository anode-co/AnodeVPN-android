package com.pkt.core.presentation.createwallet.creatingwallet

import com.pkt.core.presentation.common.state.UiEvent

sealed class CreatingWalletEvent : UiEvent {

    object Back : CreatingWalletEvent()

    object ToMain : CreatingWalletEvent()
}
