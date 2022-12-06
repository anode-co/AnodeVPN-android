package com.pkt.core.presentation.createwallet.setpin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentSetPinBinding
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
class SetPinFragment : StateFragment<CommonState.Empty>(R.layout.fragment_set_pin) {

    private val viewBinding by viewBinding(FragmentSetPinBinding::bind)

    override val viewModel: SetPinViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("SetPinFragment onViewCreated")
        with(viewBinding) {
            enterPinInputLayout.doOnTextChanged {
                viewModel.enterPin = it
            }

            confirmPinInputLayout.apply {
                clearFocusOnActionDone()

                doOnTextChanged {
                    viewModel.confirmPin = it
                }
            }

            nextButton.doOnClick {
                viewModel.onNextClick()
            }
        }
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)

        when (event) {
            is SetPinEvent.ConfirmPinError -> {
                viewBinding.confirmPinInputLayout.setError(R.string.error_pins_do_not_match)
            }
        }
    }
}
