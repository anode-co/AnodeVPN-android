package com.pkt.core.presentation.createwallet.createpassword

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.pkt.core.presentation.navigation.InternalNavigation
import com.ybs.passwordstrengthmeter.PasswordStrength

data class CreatePasswordState(
    val strength: PasswordStrength = PasswordStrength.WEAK,
    val nextButtonEnabled: Boolean = false,
) : UiState

sealed class CreatePasswordEvent : UiEvent {

    object ConfirmPasswordError : CreatePasswordEvent()
}

sealed class CreatePasswordNavigation : InternalNavigation {

    data class ToConfirmPassword(
        private val mode: CreateWalletMode,
        private val password: String,
        private val name: String?,
    ) : CreatePasswordNavigation() {
        override val navDirections: NavDirections
            get() = CreatePasswordFragmentDirections.toConfirmPassword(mode, password, name)
    }
}
