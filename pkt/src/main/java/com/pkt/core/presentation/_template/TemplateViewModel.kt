package com.pkt.core.presentation._template

import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TemplateViewModel @Inject constructor(

) : StateViewModel<TemplateState>() {

    init {}

    override fun createInitialState() = TemplateState()
}
