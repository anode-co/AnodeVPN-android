package com.pkt.core.presentation.main.settings.changepin

import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class ChangePinViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<CommonState.Empty>() {

    var enterPassword: String = ""

    var enterPin: String = ""

    var confirmPin: String = ""

    override fun createInitialState() = CommonState.Empty

    fun onChangeClick() {
        when {
            enterPin != confirmPin -> {
                sendEvent(ChangePinEvent.ConfirmPinError)
            }

            else -> {
                invokeAction {
                    walletRepository.checkWalletPassphrase(enterPassword)
                        .onSuccess {
                            walletRepository.changePin(enterPassword, enterPin)
                            sendEvent(CommonEvent.Info(R.string.success))
                            delay(1000)
                            sendNavigation(AppNavigation.OpenMain)
                        }.onFailure {
                            sendError(Throwable("Wrong password"))
                        }
                }
            }
        }
    }
}
