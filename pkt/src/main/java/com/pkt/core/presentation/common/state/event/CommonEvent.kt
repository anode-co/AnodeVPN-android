package com.pkt.core.presentation.common.state.event

import androidx.annotation.StringRes
import com.pkt.core.presentation.common.state.UiEvent

sealed class CommonEvent : UiEvent {

    data class Info(@StringRes val resId: Int) : CommonEvent()

    data class Error(val throwable: Throwable) : CommonEvent()

    data class WebUrl(val url: String) : CommonEvent()
}
