package com.pkt.core.presentation.start

import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(

) : StateViewModel<StartState>() {

    override fun createInitialState() = StartState()

    fun onCreateClick() {
        // TODO
    }

    fun onRecoverClick() {
        // TODO
    }
}
