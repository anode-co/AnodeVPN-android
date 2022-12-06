package com.pkt.core.presentation.common.state

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.pkt.core.extensions.collectLatestRepeatOnLifecycle
import com.pkt.core.presentation.common.BaseBottomSheet
import com.pkt.core.presentation.common.state.event.CommonEventHandler
import com.pkt.core.presentation.common.state.navigation.NavigationHandler
import com.pkt.core.presentation.common.state.state.CommonState
import javax.inject.Inject

abstract class StateBottomSheet<S : UiState>(contentLayoutId: Int) : BaseBottomSheet(contentLayoutId) {

    @Inject
    lateinit var navigationHandler: NavigationHandler

    protected abstract val viewModel: StateViewModel<S>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewModel) {
            collectLatestRepeatOnLifecycle(uiState) { handleState(it) }
            collectLatestRepeatOnLifecycle(uiEvent) { handleEvent(it) }
            collectLatestRepeatOnLifecycle(uiNavigation) { handleNavigation(it) }
            collectLatestRepeatOnLifecycle(loadingState) { handleLoadingState(it) }
            collectLatestRepeatOnLifecycle(actionState) { handleActionState(it) }
        }
    }

    protected open fun handleState(state: S) {

    }

    protected open fun handleEvent(event: UiEvent) {
        CommonEventHandler.handleEvent(this, event)
    }

    private fun handleNavigation(navigation: UiNavigation) {
        navigationHandler.handleNavigation(this, navigation)
    }

    private fun handleLoadingState(state: CommonState.LoadingState) {
        (view as? ConstraintLayout)?.apply {
            setLoadingProgress(state.isLoading)
            setError(state.loadingError != null) {
                state.loadingAction?.invoke()
            }
        }
    }

    private fun handleActionState(state: CommonState.ActionState) {
        (view as? ConstraintLayout)?.apply {
            setActionProgress(state.isLoading)
        }
    }
}
