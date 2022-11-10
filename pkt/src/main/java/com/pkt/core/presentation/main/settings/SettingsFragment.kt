package com.pkt.core.presentation.main.settings

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentSettingsBinding
import com.pkt.core.extensions.applyGradient
import com.pkt.core.extensions.getColorByAttribute
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.main.MainViewModel
import com.pkt.core.presentation.main.common.consent.ConsentBottomSheet
import com.pkt.core.presentation.main.settings.deletewallet.DeleteWalletBottomSheet
import com.pkt.core.presentation.main.settings.newwallet.NewWalletBottomSheet
import com.pkt.core.presentation.main.settings.renamewallet.RenameWalletBottomSheet
import com.pkt.core.presentation.main.settings.showseed.ShowSeedBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : StateFragment<SettingsState>(R.layout.fragment_settings) {

    private val viewBinding by viewBinding(FragmentSettingsBinding::bind)

    override val viewModel: SettingsViewModel by viewModels()

    private val mainViewModel: MainViewModel by activityViewModels()

    private val adapter = AsyncListDifferAdapter(
        menuAdapterDelegate {
            viewModel.onMenuItemClick(it)
        }
    ).apply {
        items = MenuItem.Type.values()
            .map { MenuItem(it, it.ordinal != MenuItem.Type.values().size - 1) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(ConsentBottomSheet.REQUEST_KEY) { _, bundle ->
            viewModel.onConsentResult(bundle.getBoolean(ConsentBottomSheet.RESULT_KEY))
        }

        setFragmentResultListener(NewWalletBottomSheet.REQUEST_KEY) { _, bundle ->
            viewModel.onNewWallet(bundle.getString(NewWalletBottomSheet.WALLET_KEY))
        }

        with(viewBinding) {
            walletButton.setOnClickListener {
                viewModel.onWalletClick()
            }
            moreButton.setOnClickListener {
                showMorePopupMenu(addButton)
            }
            addButton.setOnClickListener {
                showAddPopupMenu(it)
            }

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter
        }
    }

    private fun showMorePopupMenu(view: View) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.settings_more_popup_menu, menu)

            menu.getItem(menu.size() - 1).apply {
                title = SpannableString(title).apply {
                    setSpan(
                        ForegroundColorSpan(requireContext().getColorByAttribute(androidx.appcompat.R.attr.colorError)),
                        0,
                        length,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.rename -> viewModel.onRenameClick()
                    R.id.export -> viewModel.onExportClick()
                    R.id.delete -> viewModel.onDeleteClick()
                }
                true
            }

            gravity = Gravity.END

            show()
        }
    }

    private fun showAddPopupMenu(view: View) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.settings_add_popup_menu, menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.newWallet -> viewModel.onNewWalletClick()
                    R.id.walletRecovery -> viewModel.onWalletRecoveryClick()
                }
                true
            }

            gravity = Gravity.END

            show()
        }
    }

    override fun handleState(state: SettingsState) {
        with(viewBinding) {
            walletButton.apply {
                text = state.walletName
                applyGradient()
            }

            idLabel.text = "%s %s".format(getString(R.string.id), state.id)
            versionLabel.text = "%s %s".format(getString(R.string.version), state.version)
        }
    }

    override fun handleEvent(event: UiEvent) {
        super.handleEvent(event)

        when (event) {
            is SettingsEvent.OpenWalletInfo -> mainViewModel.openWalletInfo(event.address)
            is SettingsEvent.OpenCjdnsInfo -> mainViewModel.openCjdnsInfo()

            is SettingsEvent.OpenShowSeed -> {
                ShowSeedBottomSheet().show(childFragmentManager, ShowSeedBottomSheet.TAG)
            }

            is SettingsEvent.OpenConsent -> {
                ConsentBottomSheet().show(parentFragmentManager, ConsentBottomSheet.TAG)
            }

            is SettingsEvent.OpenRenameWallet -> {
                RenameWalletBottomSheet().show(parentFragmentManager, RenameWalletBottomSheet.TAG)
            }

            is SettingsEvent.OpenDeleteWallet -> {
                DeleteWalletBottomSheet().show(parentFragmentManager, DeleteWalletBottomSheet.TAG)
            }

            is SettingsEvent.OpenNewWallet -> {
                NewWalletBottomSheet().show(parentFragmentManager, NewWalletBottomSheet.TAG)
            }

            is SettingsEvent.OpenChangePassword -> mainViewModel.openChangePassword()
            is SettingsEvent.OpenChangePin -> mainViewModel.openChangePin()
            is SettingsEvent.OpenCreateWallet -> mainViewModel.openCreateWallet(event.name)
            is SettingsEvent.OpenRecoverWallet -> mainViewModel.openRecoverWallet(event.name)
        }
    }
}
