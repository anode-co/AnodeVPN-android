package com.pkt.core.presentation.main.settings.changepassword

import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import com.ybs.passwordstrengthmeter.PasswordStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import timber.log.Timber
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
                    Timber.d("ChangePasswordViewModel onChangeClick. Trying to change password")
                    walletRepository.changePassword(enterCurrentPassword, enterPassword)
                        .onSuccess {
                            Timber.d("Password changed successfully")
                            sendEvent(CommonEvent.Info(R.string.success))
                            delay(1000)
                            //Ask user to reset PIN
                            sendNavigation(AppNavigation.OpenChangePinFromChangePassword)
                        }
                        .onFailure {
                            Timber.e(it, "Password change failed")
                            sendEvent(CommonEvent.Warning(R.string.error_password_incorrect))
                        }
                }
            }
        }
    }
}
