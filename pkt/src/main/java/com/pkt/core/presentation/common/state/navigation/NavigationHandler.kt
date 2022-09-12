package com.pkt.core.presentation.common.state.navigation

import androidx.fragment.app.Fragment
import com.pkt.core.presentation.common.state.UiNavigation

interface NavigationHandler {

    fun handleNavigation(fragment: Fragment, navigation: UiNavigation)
}
