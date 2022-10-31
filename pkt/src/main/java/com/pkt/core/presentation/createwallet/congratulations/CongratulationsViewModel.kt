package com.pkt.core.presentation.createwallet.congratulations

import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.state.CommonState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CongratulationsViewModel @Inject constructor() : StateViewModel<CommonState.Empty>() {

    override fun createInitialState() = CommonState.Empty

    fun onNextClick() {
        sendEvent(CreatingWalletEvent.ToMain)
    }
}
