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
                    //check first if pin is available, before changing password
                    //changing password removes stored values
                    var pin: String? = null
                    if (walletRepository.isPinAvailable().getOrNull() == true) {
                        pin = walletRepository.getPin()
                    }
                    walletRepository.changePassword(enterCurrentPassword, enterPassword)
                        .onSuccess {
                            Timber.d("Password changed successfully")
                            sendEvent(CommonEvent.Info(R.string.success))
                            delay(1000)
                            //if user had a PIN, re used it for new password
                            if (pin!!.isNotEmpty()) {
                                Timber.d("PIN is available, update for changed password")
                                walletRepository.changePin(enterPassword, pin)
                            }

                            navigateBack() //to settings
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
