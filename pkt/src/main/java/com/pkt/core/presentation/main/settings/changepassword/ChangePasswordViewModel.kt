package com.pkt.core.presentation.main.settings.changepassword

import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import com.ybs.passwordstrengthmeter.PasswordStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<ChangePasswordState>() {

    var enterCurrentPassword: String = ""

    var enterPassword: String = ""
        set(value) {
            field = value
            sendState { copy(strength = PasswordStrength.calculateStrength(value)) }
        }

    var confirmPassword: String = ""

    override fun createInitialState() = ChangePasswordState()

    fun onChangeClick() {
        when {
            enterPassword != confirmPassword -> {
                sendEvent(ChangePasswordEvent.ConfirmPasswordError)
            }

            else -> {
                invokeAction {
                    walletRepository.changePassword(enterCurrentPassword, enterPassword)
                        .onSuccess {
                            sendEvent(CommonEvent.Info(R.string.success))
                            delay(1000)
                            //Ask user to reset PIN
                            //TODO: close this and changepassword if it came from there
                            sendNavigation(AppNavigation.NavigateBack)
                        }
                        .onFailure {
                            sendError(it)
                        }
                }
            }
        }
    }
}
