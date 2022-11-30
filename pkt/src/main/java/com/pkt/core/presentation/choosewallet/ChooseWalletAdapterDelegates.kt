package com.pkt.core.presentation.choosewallet

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.databinding.ItemWalletBinding
import com.pkt.core.presentation.common.adapter.DisplayableItem

data class WalletItem(
    val name: String,
    val isChecked: Boolean,
    val withDivider: Boolean,
) : DisplayableItem {
    override fun getItemId(): String = name
    override fun getItemHash(): String = hashCode().toString()
}

fun walletAdapterDelegate(
    onItemChecked: (WalletItem) -> Unit,
) =
    adapterDelegateViewBinding<WalletItem, DisplayableItem, ItemWalletBinding>(
        { layoutInflater, root -> ItemWalletBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                label.text = item.name
                divider.isVisible = item.withDivider

                radioButton.setOnCheckedChangeListener(null)
                radioButton.isChecked = item.isChecked
                radioButton.setOnCheckedChangeListener { _, _ ->
                    onItemChecked(item)
                }

                root.setOnClickListener {
                    onItemChecked(item)
                }
            }
        }
    }
