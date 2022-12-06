package com.pkt.core.presentation.createwallet.seed

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.navigation.InternalNavigation

data class SeedState(
    val seed: String? = null,
) : UiState

sealed class SeedNavigation : InternalNavigation {

    data class ToConfirmSeed(
        private val password: String,
        private val pin: String,
        private val seed: String,
        private val name: String?,
    ) : SeedNavigation() {
        override val navDirections: NavDirections
            get() = SeedFragmentDirections.toConfirmSeed(password, pin, seed, name)
    }
}
