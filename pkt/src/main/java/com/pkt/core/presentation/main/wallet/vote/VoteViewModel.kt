package com.pkt.core.presentation.main.wallet.vote

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<VoteState>() {

    private val fromaddress: String = savedStateHandle["fromaddress"]
        ?: throw IllegalArgumentException("fromAddress is required")

    private val isCandidate: Boolean = savedStateHandle["isCandidate"]
        ?: false

    var toaddress: String = ""
        set(value) {
            field = value
            invalidateVoteButtonState()
        }

    init {
        sendState { copy(candidateSelected = isCandidate) }
    }

    override fun createInitialState() = VoteState()

    fun onCandidateCheckChanged(checked: Boolean) {
        sendState { copy(candidateSelected = checked) }
        invalidateVoteButtonState()
    }

    fun onWithdrawVoteCheckChanged(checked: Boolean) {
        sendState { copy(withdrawVoteSelected = checked) }
        invalidateVoteButtonState()
    }

    fun onVoteClick() {
        invokeAction {
            if (currentState.candidateSelected) {
                Timber.d("VoteViewModel onVoteClick| Candidate selected")
            }
            if (currentState.withdrawVoteSelected) {
                Timber.d("VoteViewModel onVoteClick| withdrawVote selected")
            }
            runCatching {
                if (!currentState.withdrawVoteSelected) {
                    toaddress = walletRepository.isPKTAddressValid(toaddress).getOrThrow()
                }
            }.onSuccess {
                Timber.i("VoteViewModel onVoteClick| PKT address is valid")
                walletRepository.sendVote(
                    fromAddress = fromaddress,
                    voteFor = toaddress,
                    isCandidate = currentState.candidateSelected
                ).onSuccess {
                    Timber.i("VoteViewModel onVoteClick| Success")
                    sendEvent(CommonEvent.Info(R.string.vote_submitted))
                    navigateBack()
                }.onFailure {
                    Timber.e(it, "VoteViewModel onVoteClick| Error")
                    sendError(it)
                }
            }.onFailure {
                Timber.i("VoteViewModel onVoteClick| PKT address is invalid")
                sendEvent(VoteEvent.AddressError(it.message))
            }
        }
    }

    private fun invalidateVoteButtonState() {
        sendState {
            copy(
                voteButtonEnabled = toaddress.isNotBlank() || currentState.withdrawVoteSelected
            )
        }

    }
}
