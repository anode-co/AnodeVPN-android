package com.pkt.core.presentation.createwallet.confirmseed

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.navigation.InternalNavigation

data class ConfirmSeedState(
    val wordPosition: Int = 0,
    val nextButtonEnabled: Boolean = false,
) : UiState

sealed class ConfirmSeedEvent : UiEvent {

    object ConfirmSeedError : ConfirmSeedEvent()
}

sealed class ConfirmSeedNavigation : InternalNavigation {

    data class ToCreateWallet(
        private val password: String,
        private val pin: String,
        private val seed: String,
    ) : ConfirmSeedNavigation() {
        override val navDirections: NavDirections
            get() = ConfirmSeedFragmentDirections.toCreatingWallet(password, pin, seed)
    }
}
