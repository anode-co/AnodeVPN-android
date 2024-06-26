package com.pkt.core.presentation.createwallet.seed

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.domain.repository.WalletRepository
import com.pkt.domain.repository.GeneralRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val generalRepository: GeneralRepository,
) : StateViewModel<SeedState>() {

    private val password: String = savedStateHandle["password"] ?: throw IllegalArgumentException("password required")
    private val pin: String = savedStateHandle["pin"] ?: throw IllegalArgumentException("pin required")
    private val name: String? = savedStateHandle["name"]


    init {
        invokeLoadingAction {
            var walletName = "wallet"
            if (name != null) {
                walletName = name
            }

            withContext(Dispatchers.IO) {
                generalRepository.createPldWallet(password, pin , walletName)
                    .onSuccess {
                        Timber.d("SeedViewModel| Seed generated successfully")
                        generalRepository.launchPLD(walletName)
                        walletRepository.setActiveWallet(walletName)
                        withContext(Dispatchers.Main) {
                            sendState { copy(seed = it) }
                        }
                    }.onFailure {
                        generalRepository.launchPLD(walletName)
                        Timber.e(it, "SeedViewModel| Seed generation failed")
                        withContext(Dispatchers.Main) {
                            sendError(it)
                        }
                    }
            }
        }
    }

    override fun createInitialState() = SeedState()

    fun onCopyClick() {
        currentState.seed?.let {
            sendEvent(CommonEvent.CopyToBuffer(R.string.your_seed_phrase, it))
            sendEvent(CommonEvent.Info(R.string.seed_phrase_copied))
        }
    }

    fun onNextClick() {
        sendNavigation(SeedNavigation.ToConfirmSeed(password, pin, currentState.seed!!, name))
    }
}
