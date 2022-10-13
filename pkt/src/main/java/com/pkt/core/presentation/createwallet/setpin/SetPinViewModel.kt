package com.pkt.core.presentation.createwallet.setpin

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetPinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : StateViewModel<SetPinState>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")

    var enterPin: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    var confirmPin: String = ""
        set(value) {
            field = value
            invalidateNextButtonEnabled()
        }

    override fun createInitialState() = SetPinState()

    private fun invalidateNextButtonEnabled() {
        sendState {
            copy(
                nextButtonEnabled = enterPin.length >= PIN_MIN_LENGTH && confirmPin.length >= PIN_MIN_LENGTH
            )
        }
    }

    fun onNextClick() {
        when {
            enterPin != confirmPin -> {
                sendEvent(SetPinEvent.ConfirmPinError)
            }

            else -> {
                sendNavigation(SetPinNavigation.ToSeed(password, enterPin))
            }
        }
    }

    private companion object {
        const val PIN_MIN_LENGTH = 4
    }
}
