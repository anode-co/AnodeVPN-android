package com.pkt.core.presentation.main.settings.changepin

import com.pkt.core.presentation.common.state.UiEvent

sealed class ChangePinEvent : UiEvent {

    object ConfirmPinError : ChangePinEvent()
}
