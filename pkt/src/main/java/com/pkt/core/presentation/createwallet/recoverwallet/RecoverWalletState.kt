package com.pkt.core.presentation.createwallet.recoverwallet

import androidx.navigation.NavDirections
import com.pkt.core.presentation.common.state.UiState
import com.pkt.core.presentation.navigation.InternalNavigation

data class RecoverWalletState(
    val nextButtonEnabled: Boolean = false,
    val seedError: Int? = null,
) : UiState

sealed class RecoverWalletNavigation : InternalNavigation {

    object ToCongratulations : RecoverWalletNavigation() {
        override val navDirections: NavDirections
            get() = RecoverWalletFragmentDirections.toCongratulations()
    }
}
