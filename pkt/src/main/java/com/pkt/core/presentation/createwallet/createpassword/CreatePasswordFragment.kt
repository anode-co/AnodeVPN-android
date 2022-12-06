package com.pkt.core.presentation.createwallet.createpassword

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentCreatePasswordBinding
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.util.IntentUtil
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CreatePasswordFragment : StateFragment<CreatePasswordState>(R.layout.fragment_create_password) {

    private val viewBinding by viewBinding(FragmentCreatePasswordBinding::bind)

    override val viewModel: CreatePasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("CreatePasswordFragment onViewCreated")
        with(viewBinding) {
            enterPasswordInputLayout.doOnTextChanged {
                viewModel.enterPassword = it
            }

            confirmPasswordInputLayout.apply {
                clearFocusOnActionDone()

                doOnTextChanged {
                    viewModel.confirmPassword = it
                }
            }

            checkbox1.doOnCheckChanged {
                viewModel.checkbox1Checked = it
            }

            checkbox2.doOnCheckChanged {
                viewModel.checkbox2Checked = it
                viewModel.onDataConsentClick()
            }

            val privacyPolicyText = SpannableString(getString(R.string.create_password_checkbox_2)).apply {
                setClickOnText(
                    text = getString(R.string.privacy_policy),
                    color = requireContext().getColorByAttribute(android.R.attr.colorPrimary),
                    isUnderlineText = false
                ) {
                    clearFocus()
                    IntentUtil.openWebUrl(requireContext(), getString(R.string.privacy_policy_url))
                }
            }
            checkbox2Label.apply {
                text = privacyPolicyText
                highlightColor = Color.TRANSPARENT
                movementMethod = LinkMovementMethod.getInstance()
            }

            nextButton.doOnClick {
                viewModel.onNextClick()
            }
        }
    }

    override fun handleState(state: CreatePasswordState) {
        with(viewBinding) {
            strengthView.strength = state.strength
            nextButton.isEnabled = state.nextButtonEnabled
        }
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)

        when (event) {
            is CreatePasswordEvent.ConfirmPasswordError -> {
                viewBinding.confirmPasswordInputLayout.setError(R.string.error_passwords_do_not_match)
            }
        }
    }
}
