package com.pkt.core.presentation.createwallet

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.navigation.AppNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateWalletViewModel @Inject constructor(

) : StateViewModel<CommonState.Empty>() {

    override fun createInitialState() = CommonState.Empty

    fun toMain() {
        sendNavigation(AppNavigation.OpenMain)
    }
}
