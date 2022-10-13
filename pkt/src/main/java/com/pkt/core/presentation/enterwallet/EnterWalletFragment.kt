package com.pkt.core.presentation.enterwallet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentEnterWalletBinding
import com.pkt.core.extensions.clearFocusOnActionDone
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.doOnTextChanged
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.enterwallet.choosewallet.ChooseWalletBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil

@AndroidEntryPoint
class EnterWalletFragment : StateFragment<EnterWalletState>(R.layout.fragment_enter_wallet) {

    private val viewBinding by viewBinding(FragmentEnterWalletBinding::bind)

    override val viewModel: EnterWalletViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(ChooseWalletBottomSheet.REQUEST_KEY) { _, bundle ->
            viewModel.onWalletChanged(bundle.getString(ChooseWalletBottomSheet.WALLET_KEY))
        }

        with(viewBinding) {
            walletButton.doOnClick {
                viewModel.onChooseWalletClick()
            }

            pinDeleteButton.setOnClickListener {
                viewModel.onPinDeleteClick()
            }

            pinKeyboard.onKeyClick = {
                viewModel.onKeyClick(it)
            }

            passwordInputLayout.apply {
                clearFocusOnActionDone()

                doOnTextChanged {
                    viewModel.password = it
                }
            }

            loginButton.doOnClick {
                viewModel.onLoginClick()
            }
        }
    }

    override fun handleState(state: EnterWalletState) {
        with(viewBinding) {
            walletButton.apply {
                text = state.currentWalletName
                isVisible = state.wallets.size > 1
            }

            pinValue.text = state.pin

            loginButton.isEnabled = state.loginButtonEnabled

            if (state.isPinAvailable) {
                pinInputLabel.isVisible = true
                pinInputLayout.isVisible = true
                pinKeyboard.isVisible = true

                passwordInputLabel.isVisible = false
                passwordInputLayout.isVisible = false
                loginButton.isVisible = false
            } else {
                pinInputLabel.isVisible = false
                pinInputLayout.isVisible = false
                pinKeyboard.isVisible = false

                passwordInputLabel.isVisible = true
                passwordInputLayout.isVisible = true
                loginButton.isVisible = true
            }
        }
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is EnterWalletEvent.ClearPassword -> {
                viewBinding.passwordInput.setText("")
            }
            is EnterWalletEvent.ShowKeyboard -> {
                viewBinding.passwordInput.apply {
                    requestFocus()
                    UIUtil.showKeyboard(requireContext(), this)
                }

            }
            is EnterWalletEvent.OpenChooseWallet -> {
                ChooseWalletBottomSheet.newInstance(event.wallets, event.currentWallet)
                    .show(parentFragmentManager, ChooseWalletBottomSheet.TAG)
            }
            else -> {
                super.handleEvent(event)
            }
        }
    }
}
