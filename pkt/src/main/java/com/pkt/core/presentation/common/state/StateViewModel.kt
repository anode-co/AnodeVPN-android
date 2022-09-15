package com.pkt.core.presentation.common.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.core.presentation.common.state.event.CommonEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class StateViewModel<S : UiState> : ViewModel() {

    // State section
    private val _uiState: MutableStateFlow<S> by lazy { MutableStateFlow(createInitialState()) }
    val uiState: Flow<S> by lazy { _uiState }

    private val _uiEvent: Channel<UiEvent> = Channel(Channel.CONFLATED)
    val uiEvent: Flow<UiEvent> = _uiEvent.receiveAsFlow()

    private val _uiNavigation: Channel<UiNavigation> = Channel(Channel.CONFLATED)
    val uiNavigation: Flow<UiNavigation> = _uiNavigation.receiveAsFlow()

    protected val currentState: S
        get() = _uiState.value

    protected abstract fun createInitialState(): S

    protected fun sendState(reduce: S.() -> S) {
        _uiState.tryEmit(currentState.reduce())
    }

    protected fun sendEvent(event: UiEvent) {
        _uiEvent.trySend(event)
    }

    protected fun sendError(throwable: Throwable) {
        sendEvent(CommonEvent.Error(throwable))
    }

    protected fun openWebUrl(url: String) {
        sendEvent(CommonEvent.WebUrl(url))
    }

    protected fun sendNavigation(navigation: UiNavigation) {
        _uiNavigation.trySend(navigation)
    }

    fun navigateBack() {
        sendNavigation(AppNavigation.NavigateBack)
    }

    // Loading section
    private val _loadingState: MutableStateFlow<CommonState.LoadingState> by lazy { MutableStateFlow(CommonState.LoadingState()) }
    val loadingState: Flow<CommonState.LoadingState> by lazy { _loadingState }

    private val loadingAction: (suspend () -> Result<*>)? by lazy { createLoadingAction() }

    protected open fun createLoadingAction(): (suspend () -> Result<*>)? = null

    private fun sendLoadingState(reduce: CommonState.LoadingState.() -> CommonState.LoadingState) {
        _loadingState.tryEmit(_loadingState.value.reduce())
    }

    protected fun invokeLoadingAction() {
        loadingAction?.let { action ->
            viewModelScope.launch {
                sendLoadingState { copy(isLoading = true, loadingError = null, loadingAction = null) }

                action()
                    .onSuccess {
                        sendLoadingState { copy(isLoading = false) }
                    }
                    .onFailure {
                        sendLoadingState { copy(loadingError = it, loadingAction = { invokeLoadingAction() }) }
                    }
            }
        }
    }

    fun invokeRefreshingAction() {
        loadingAction?.let { action ->
            viewModelScope.launch {
                sendLoadingState { copy(isRefreshing = true) }

                action()
                    .onFailure {
                        sendError(it)
                    }

                sendLoadingState { copy(isRefreshing = false) }
            }
        }
    }

    protected fun invokeSilentLoadingAction() {
        loadingAction?.let { action ->
            viewModelScope.launch {
                action()
                    .onSuccess {
                        sendLoadingState { copy(isLoading = false) }
                    }
                    .onFailure {
                        sendLoadingState { copy(loadingError = it, loadingAction = { invokeLoadingAction() }) }
                    }
            }
        }
    }

    fun startTimer() {
        //invokeRefreshingAction every minute
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                invokeRefreshingAction()
            }
        }
    }
}
