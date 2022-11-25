package com.pkt.core.presentation.createwallet.setpin

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.createwallet.CreateWalletMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetPinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<CommonState.Empty>() {

    private val mode: CreateWalletMode = savedStateHandle["mode"] ?: CreateWalletMode.CREATE
    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private var name: String = savedStateHandle["name"] ?: ""

    var enterPin: String = ""

    var confirmPin: String = ""

    override fun createInitialState() = CommonState.Empty

    fun onNextClick() {
        if (name.isEmpty()) {
            name = "wallet"
        }
        when {
            enterPin != confirmPin -> {
                sendEvent(SetPinEvent.ConfirmPinError)
            }

            mode == CreateWalletMode.CREATE -> {
                sendNavigation(SetPinNavigation.ToSeed(password, enterPin, name))
            }

            else -> {
                sendNavigation(SetPinNavigation.ToRecoverWallet(password, enterPin, name))
            }
        }
    }
}
