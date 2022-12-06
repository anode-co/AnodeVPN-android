package com.pkt.core.presentation.main.settings.changepin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentChangePinBinding
import com.pkt.core.extensions.clearFocusOnActionDone
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.doOnTextChanged
import com.pkt.core.extensions.setError
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.state.CommonState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ChangePinFragment : StateFragment<CommonState.Empty>(R.layout.fragment_change_pin) {

    private val viewBinding by viewBinding(FragmentChangePinBinding::bind)

    override val viewModel: ChangePinViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("ChangePinFragment onViewCreated")
        with(viewBinding) {
            enterPasswordInputLayout.doOnTextChanged {
                viewModel.enterPassword = it
            }

            enterPinInputLayout.doOnTextChanged {
                viewModel.enterPin = it
            }

            confirmPinInputLayout.apply {
                clearFocusOnActionDone()

                doOnTextChanged {
                    viewModel.confirmPin = it
                }
            }

            changeButton.doOnClick {
                viewModel.onChangeClick()
            }
        }
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)

        when (event) {
            is ChangePinEvent.ConfirmPinError -> {
                viewBinding.confirmPinInputLayout.setError(R.string.error_pins_do_not_match)
            }
        }
    }
}
