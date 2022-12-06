package com.pkt.core.presentation.createwallet.confirmseed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentConfirmSeedBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import timber.log.Timber

@AndroidEntryPoint
class ConfirmSeedFragment : StateFragment<ConfirmSeedState>(R.layout.fragment_confirm_seed) {

    private val viewBinding by viewBinding(FragmentConfirmSeedBinding::bind)

    override val viewModel: ConfirmSeedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("ConfirmSeedFragment onViewCreated")
        with(viewBinding) {
            wordInputLayout.apply {
                clearFocusOnActionDone()

                doOnTextChanged {
                    viewModel.word = it
                }
            }

            nextButton.doOnClick {
                viewModel.onNextClick()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        with(viewBinding.wordInput) {
            requestFocus()
            UIUtil.showKeyboard(requireContext(), this)
        }
    }

    override fun handleState(state: ConfirmSeedState) {
        with(viewBinding) {
            titleLabel.text = getString(R.string.type_word, (state.wordPosition + 1).formatPosition(requireContext()))
            nextButton.isEnabled = state.nextButtonEnabled
        }
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)

        when (event) {
            is ConfirmSeedEvent.ConfirmSeedError -> {
                viewBinding.wordInputLayout.setError(R.string.error_word_incorrect)
            }
        }
    }
}
