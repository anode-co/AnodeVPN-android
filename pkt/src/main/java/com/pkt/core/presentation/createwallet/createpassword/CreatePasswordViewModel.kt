package com.pkt.core.presentation.createwallet.createpassword

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.createwallet.CreateWalletMode
import com.ybs.passwordstrengthmeter.PasswordStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreatePasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<CreatePasswordState>() {

    private val mode: CreateWalletMode = savedStateHandle["mode"] ?: CreateWalletMode.CREATE
    private val name: String? = savedStateHandle["name"]

    var enterPassword: String = ""
        set(value) {
            field = value
            sendState { copy(strength = PasswordStrength.calculateStrength(value)) }
        }

    var confirmPassword: String = ""

    var checkbox1Checked: Boolean = false
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    var checkbox2Checked: Boolean = false

    override fun createInitialState() = CreatePasswordState()

    private fun invalidateNextButtonEnabled() {
        sendState {
            copy(
                nextButtonEnabled = checkbox1Checked
            )
        }
    }

    fun onNextClick() {
        when {
            enterPassword != confirmPassword -> {
                sendEvent(CreatePasswordEvent.ConfirmPasswordError)
            }
            else -> {
                sendNavigation(CreatePasswordNavigation.ToConfirmPassword(mode, enterPassword, name))
            }
        }
    }
}
