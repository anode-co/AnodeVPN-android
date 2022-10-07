package com.pkt.core.presentation.start

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.navigation.AppNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(

) : StateViewModel<CommonState.Empty>() {

    override fun createInitialState() = CommonState.Empty

    fun onCreateClick() {
        sendNavigation(AppNavigation.OpenCreateWallet)
    }

    fun onRecoverClick() {
        sendNavigation(AppNavigation.OpenRecoverWallet)
    }
}
