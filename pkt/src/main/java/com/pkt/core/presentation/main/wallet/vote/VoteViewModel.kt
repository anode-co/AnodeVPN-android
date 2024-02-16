package com.pkt.core.presentation.main.wallet.vote

import androidx.lifecycle.SavedStateHandle
import com.pkt.core.R
import com.pkt.core.extensions.toPKT
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.navigation.AppNavigation
import com.pkt.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SendTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : StateViewModel<VoteState>() {

    private val fromaddress: String = savedStateHandle["fromaddress"]
        ?: throw IllegalArgumentException("fromAddress is required")
    var toaddress: String = ""
        set(value) {
            field = value
            invalidateVoteButtonState()
        }

    private var balance = 0L

    init {
        invokeLoadingAction {
            runCatching {
                balance = walletRepository.getWalletBalance(fromaddress).getOrNull()!!
            }
        }
    }

    override fun createInitialState() = VoteState()

    fun onCandidateCheckChanged(checked: Boolean) {
        sendState { copy(candidateSelected = checked) }
        invalidateVoteButtonState()

        /*if (!checked) {
            sendEvent(SendTransactionEvent.OpenKeyboard)
        }*/
    }

    fun onWithdrawVoteCheckChanged(checked: Boolean) {
        sendState { copy(withdrawVoteSelected = checked) }
        invalidateVoteButtonState()

        if (!checked) {
            sendEvent(VoteEvent.OpenKeyboard)
        }
    }

    fun onVoteClick() {
        if (currentState.candidateSelected) {
            Timber.d("VoteViewModel onVoteClick| Candidate selected")
        }
        if (currentState.withdrawVoteSelected) {
            Timber.d("VoteViewModel onVoteClick| withdrawVote selected")
        }
        runCatching {
            toaddress = walletRepository.isPKTAddressValid(toaddress).getOrThrow()
        }.onSuccess {
            Timber.i("VoteViewModel onVoteClick| PKT address is valid")
            //TODO: update this event
            /*sendNavigation(
                AppNavigation.OpenSendConfirm(
                    fromaddress = fromaddress,
                    toaddress = toaddress,

                )
            )*/
        }.onFailure {
            Timber.i("VoteViewModel onVoteClick| PKT address is invalid")
            sendEvent(VoteEvent.AddressError(it.message))
        }
    }

    private fun invalidateVoteButtonState() {
        sendState {
            copy(
                voteButtonEnabled = toaddress.isNotBlank()
            )
        }
    }
}
