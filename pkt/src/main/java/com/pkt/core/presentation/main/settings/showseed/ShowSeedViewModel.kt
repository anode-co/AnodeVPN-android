package com.pkt.core.presentation.main.settings.showseed

import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ShowSeedViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : StateViewModel<ShowSeedState>() {

    override fun createInitialState() = ShowSeedState(seed = runBlocking { walletRepository.getSeed().getOrNull() })

    fun onCopyClick() {
        currentState.seed?.let { seed ->
            sendEvent(CommonEvent.CopyToBuffer(R.string.your_seed_phrase_2, seed))
            sendEvent(CommonEvent.Info(R.string.seed_phrase_copied_2))
        }
    }
}
