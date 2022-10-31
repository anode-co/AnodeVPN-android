package com.pkt.core.presentation.createwallet.setpin

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.navigation.InternalNavigation

sealed class SetPinEvent : UiEvent {

    object ConfirmPinError : SetPinEvent()
}

sealed class SetPinNavigation : InternalNavigation {

    data class ToSeed(
        private val password: String,
        private val pin: String,
        private val name: String?,
    ) : SetPinNavigation() {
        override val navDirections: NavDirections
            get() = SetPinFragmentDirections.toSeed(password, pin, name)
    }

    data class ToRecoverWallet(private val password: String, private val pin: String) : SetPinNavigation() {
        override val navDirections: NavDirections
            get() = SetPinFragmentDirections.toRecoverWallet(password, pin)
    }
}
