package com.pkt.core.presentation.common.state.event

import androidx.fragment.app.Fragment
import com.pkt.core.extensions.copyToBuffer
import com.pkt.core.extensions.showNotification
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.util.IntentUtil

object CommonEventHandler {

    fun handleEvent(fragment: Fragment, event: UiEvent) {
        when (event) {
            is CommonEvent.WebUrl -> {
                IntentUtil.openWebUrl(fragment.requireContext(), event.url)
            }

            is CommonEvent.CopyToBuffer -> {
                fragment.requireContext().copyToBuffer(
                    labelResId = event.labelResId,
                    text = event.text
                )
            }

            is CommonEvent.Notification -> {
                //TODO: handle custom errors to the user here
                fragment.showNotification(event.textResId, event.type)
            }
        }
    }
}
