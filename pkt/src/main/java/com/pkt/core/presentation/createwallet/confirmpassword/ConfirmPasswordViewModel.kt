package com.pkt.core.presentation.createwallet.confirmpassword

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.createwallet.CreateWalletMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfirmPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<ConfirmPasswordState>() {

    private val mode: CreateWalletMode = savedStateHandle["mode"] ?: CreateWalletMode.CREATE
    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private val name: String = savedStateHandle["name"] ?: ""

    var checkboxChecked: Boolean = false
        set(value) {
            field = value

            sendState { copy(nextButtonEnabled = value) }
        }

    override fun createInitialState() = ConfirmPasswordState()

    fun onNextClick() {
        sendNavigation(ConfirmPasswordNavigation.ToSetPin(mode, password, name))
    }
}
