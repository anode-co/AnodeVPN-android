package com.pkt.core.presentation.createwallet.recoverwallet

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentRecoverWalletBinding
import com.pkt.core.extensions.clearFocusOnActionDone
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.doOnTextChanged
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import timber.log.Timber

@AndroidEntryPoint
class RecoverWalletFragment : StateFragment<RecoverWalletState>(R.layout.fragment_recover_wallet) {

    private val viewBinding by viewBinding(FragmentRecoverWalletBinding::bind)

    override val viewModel: RecoverWalletViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("RecoverWalletFragment onViewCreated")
        with(viewBinding) {
            seedInput.doAfterTextChanged {
                viewModel.seed = it.toString()
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
            seedInputLayout.apply {
                error = state.seedError?.let { getString(it) }
                isErrorEnabled = state.seedError != null
            }
        }
    }
}
