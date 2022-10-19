package com.pkt.core.presentation.common.state

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.pkt.core.R
import com.pkt.core.extensions.applyGradient
import com.pkt.core.extensions.collectLatestRepeatOnLifecycle
import com.pkt.core.extensions.getColorByAttribute
import com.pkt.core.extensions.scrollWhenOpenKeyboard
import com.pkt.core.presentation.common.state.event.CommonEventHandler
import com.pkt.core.presentation.common.state.navigation.NavigationHandler
import com.pkt.core.presentation.common.state.state.CommonState
import javax.inject.Inject

abstract class StateFragment<S : UiState>(contentLayoutId: Int) : Fragment(contentLayoutId) {

    @Inject
    lateinit var navigationHandler: NavigationHandler

    private var toolbar: MaterialToolbar? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var scrollView: NestedScrollView? = null

    protected abstract val viewModel: StateViewModel<S>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        scrollView = view.findViewById(R.id.scrollView)

        toolbar?.apply {
            applyGradient()
            setNavigationOnClickListener {
                viewModel.navigateBack()
            }
        }

        swipeRefreshLayout?.apply {
            setColorSchemeColors(requireContext().getColorByAttribute(android.R.attr.colorPrimary))

            setOnRefreshListener {
                viewModel.invokeRefreshingAction()
            }
        }

        scrollView?.let {
            scrollWhenOpenKeyboard(it)
        }

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

        swipeRefreshLayout?.isRefreshing = state.isRefreshing
    }

    private fun handleActionState(state: CommonState.ActionState) {
        (view as? ConstraintLayout)?.apply {
            setActionProgress(state.isLoading)
        }
    }
}
