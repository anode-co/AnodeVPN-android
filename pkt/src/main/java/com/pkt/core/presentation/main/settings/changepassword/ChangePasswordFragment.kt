package com.pkt.core.presentation.main.settings.changepassword

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentChangePasswordBinding
import com.pkt.core.extensions.clearFocusOnActionDone
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.doOnTextChanged
import com.pkt.core.extensions.setError
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ChangePasswordFragment : StateFragment<ChangePasswordState>(R.layout.fragment_change_password) {

    private val viewBinding by viewBinding(FragmentChangePasswordBinding::bind)

    override val viewModel: ChangePasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("ChangePasswordFragment onViewCreated")
        with(viewBinding) {
            enterCurrentPasswordInputLayout.doOnTextChanged {
                viewModel.enterCurrentPassword = it
            }

            enterPasswordInputLayout.doOnTextChanged {
                viewModel.enterPassword = it
            }

            confirmPasswordInputLayout.apply {
                clearFocusOnActionDone()

                doOnTextChanged {
                    viewModel.confirmPassword = it
                }
            }

            changeButton.doOnClick {
                viewModel.onChangeClick()
            }
        }
    }

    override fun handleState(state: ChangePasswordState) {
        with(viewBinding) {
            strengthView.strength = state.strength
        }
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)

        when (event) {
            is ChangePasswordEvent.ConfirmPasswordError -> {
                viewBinding.confirmPasswordInputLayout.setError(R.string.error_passwords_do_not_match)
            }
        }
    }
}
