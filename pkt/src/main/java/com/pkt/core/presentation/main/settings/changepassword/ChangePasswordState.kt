package com.pkt.core.presentation.main.settings.changepassword

import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.UiState
import com.ybs.passwordstrengthmeter.PasswordStrength

data class ChangePasswordState(
    val strength: PasswordStrength = PasswordStrength.WEAK,
) : UiState

sealed class ChangePasswordEvent : UiEvent {

    object ConfirmPasswordError : ChangePasswordEvent()
}
