package com.pkt.core.presentation.common.state.event

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pkt.core.R
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.util.IntentUtil

object CommonEventHandler {

    fun handleEvent(fragment: Fragment, event: UiEvent) {
        when (event) {
            is CommonEvent.Info -> {
                Toast.makeText(fragment.requireContext(), event.resId, Toast.LENGTH_SHORT).show()
            }

            is CommonEvent.Error -> {
                Toast.makeText(fragment.requireContext(), R.string.error_common, Toast.LENGTH_SHORT).show()
            }

            is CommonEvent.WebUrl -> {
                IntentUtil.openWebUrl(fragment.requireContext(), event.url)
            }
        }
    }
}
