package com.pkt.core.presentation.common.state.event

import androidx.annotation.StringRes
import com.pkt.core.R
import com.pkt.core.presentation.common.state.UiEvent

sealed class CommonEvent : UiEvent {

    class Info(@StringRes textResId: Int) : Notification(textResId, Type.SUCCESS)

    class Warning(@StringRes textResId: Int) : Notification(textResId, Type.FAILURE)

    data class Error(val throwable: Throwable) : Notification(R.string.error_common, Type.FAILURE)

    data class WebUrl(val url: String) : CommonEvent()

    data class CopyToBuffer(@StringRes val labelResId: Int, val text: String) : CommonEvent()

    open class Notification(@StringRes val textResId: Int, val type: Type) : CommonEvent() {

        enum class Type {
            SUCCESS,
            FAILURE
        }
    }
}
