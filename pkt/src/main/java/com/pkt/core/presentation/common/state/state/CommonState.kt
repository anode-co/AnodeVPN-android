package com.pkt.core.presentation.common.state.state

import com.pkt.core.presentation.common.state.UiState

sealed class CommonState : UiState {

    object Empty : CommonState()

    data class LoadingState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val loadingError: Throwable? = null,
        val loadingAction: (() -> Unit)? = null,
    ) : CommonState()

    data class ActionState(
        val isLoading: Boolean = false,
    ) : CommonState()
}
