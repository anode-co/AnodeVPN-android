package com.pkt.core.presentation.main.vpn

import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VpnViewModel @Inject constructor(

) : StateViewModel<VpnState>() {

    init {
        // TODO
    }

    override fun createInitialState() = VpnState()
}
