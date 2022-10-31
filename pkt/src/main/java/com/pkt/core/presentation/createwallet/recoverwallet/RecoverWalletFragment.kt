package com.pkt.core.presentation.createwallet.recoverwallet

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentRecoverWalletBinding
import com.pkt.core.extensions.clearFocusOnActionDone
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.doOnTextChanged
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil

@AndroidEntryPoint
class RecoverWalletFragment : StateFragment<RecoverWalletState>(R.layout.fragment_recover_wallet) {

    private val viewBinding by viewBinding(FragmentRecoverWalletBinding::bind)

    override val viewModel: RecoverWalletViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            seedInputLayout.doOnTextChanged {
                viewModel.seed = it
            }

            seedInput.apply {
                imeOptions = EditorInfo.IME_ACTION_NEXT
                setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            }

            passwordInputLayout.apply {
                clearFocusOnActionDone()

                doOnTextChanged {
                    viewModel.seedPassword = it
                }
            }

            nextButton.doOnClick {
                viewModel.onNextClick()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        with(viewBinding) {
            if (!seedInput.hasFocus() && !passwordInput.hasFocus()) {
                seedInput.requestFocus()
                UIUtil.showKeyboard(requireContext(), seedInput)
            }
        }
    }

    override fun handleState(state: RecoverWalletState) {
        with(viewBinding) {
            nextButton.isEnabled = state.nextButtonEnabled
        }
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)

        when (event) {
            is RecoverWalletEvent.SeedError -> {
                viewBinding.seedInputLayout.error = event.error
            }

            is RecoverWalletEvent.PasswordError -> {
                viewBinding.passwordInputLayout.error = event.error
            }
        }
    }
}
