package com.pkt.core.presentation.createwallet.confirmpassword

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.navigation.InternalNavigation

data class ConfirmPasswordState(
    val nextButtonEnabled: Boolean = false,
) : UiState

sealed class ConfirmPasswordNavigation : InternalNavigation {

    data class ToSetPin(
        val mode: CreateWalletMode,
        private val password: String,
    ) : ConfirmPasswordNavigation() {
        override val navDirections: NavDirections
            get() = ConfirmPasswordFragmentDirections.toSetPin(mode, password)
    }
}
