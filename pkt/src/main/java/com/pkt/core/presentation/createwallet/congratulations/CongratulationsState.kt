package com.pkt.core.presentation.createwallet.congratulations

import com.pkt.core.presentation.common.state.UiEvent

sealed class CreatingWalletEvent : UiEvent {

    object ToMain : CreatingWalletEvent()
}
