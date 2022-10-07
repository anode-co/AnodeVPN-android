package com.pkt.core.presentation.createwallet.confirmpassword

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfirmPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<ConfirmPasswordState>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")

    var checkboxChecked: Boolean = false
        set(value) {
            field = value

            sendState { copy(nextButtonEnabled = value) }
        }

    override fun createInitialState() = ConfirmPasswordState()

    fun onNextClick() {
        sendNavigation(ConfirmPasswordNavigation.ToSetPin(password))
    }
}
