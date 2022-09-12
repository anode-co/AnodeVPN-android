package com.pkt.core.presentation.main.settings.walletinfo

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentWalletInfoBinding
import com.pkt.core.extensions.applyGradient
import com.pkt.core.extensions.formatSeconds
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletInfoFragment : StateFragment<WalletInfoState>(R.layout.fragment_wallet_info) {

    private val viewBinding by viewBinding(FragmentWalletInfoBinding::bind)

    override val viewModel: WalletInfoViewModel by viewModels()

    private val adapter = AsyncListDifferAdapter(
        keyValueVerticalAdapterDelegate(),
        connectedServersAdapterDelegate(),
        connectedServerAdapterDelegate()
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            toolbarTitleLabel.applyGradient()

            moreButton.setOnClickListener {
                showPopupMenu(it)
            }

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter
        }

        collectLatestRepeatOnLifecycle(viewModel.timerUiState) {
            viewBinding.toolbarSubtitleLabel.apply {
                text = getString(R.string.settings_taken, it.formatSeconds())
                isVisible = it != null
            }
        }
    }

    private fun showPopupMenu(view: View) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.wallet_info_popup_menu, menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.details -> viewModel.onDetailsClick()
                    R.id.debugLogs -> viewModel.onDebugLogsClick()
                }
                true
            }

            gravity = Gravity.END

            show()
        }
    }

    override fun handleState(state: WalletInfoState) {
        adapter.items = state.items
    }
}
