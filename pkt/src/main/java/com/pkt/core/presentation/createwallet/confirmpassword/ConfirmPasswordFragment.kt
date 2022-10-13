package com.pkt.core.presentation.createwallet.confirmpassword

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentConfirmPasswordBinding
import com.pkt.core.extensions.doOnCheckChanged
import com.pkt.core.extensions.doOnClick
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmPasswordFragment : StateFragment<ConfirmPasswordState>(R.layout.fragment_confirm_password) {

    private val viewBinding by viewBinding(FragmentConfirmPasswordBinding::bind)

    override val viewModel: ConfirmPasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            checkbox.doOnCheckChanged {
                viewModel.checkboxChecked = it
            }

            nextButton.doOnClick {
                viewModel.onNextClick()
            }
        }
    }

    override fun handleState(state: ConfirmPasswordState) {
        with(viewBinding) {
            nextButton.isEnabled = state.nextButtonEnabled
        }
    }
}
