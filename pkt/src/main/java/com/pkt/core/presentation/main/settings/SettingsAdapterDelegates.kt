package com.pkt.core.presentation.main.settings

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.R
import com.pkt.core.databinding.ItemMenuBinding
import com.pkt.core.presentation.common.adapter.DisplayableItem

data class MenuItem(
    val type: Type,
    val showDivider: Boolean,
) : DisplayableItem {
    override fun getItemId(): String = type.toString()
    override fun getItemHash(): String = hashCode().toString()

    enum class Type {
        CHANGE_PASSWORD,
        CHANGE_PIN,
        SHOW_SEED,
        CJDNS_INFO,
        WALLET_INFO,
        DATA_CONSENT;

        val titleResId: Int
            get() = when (this) {
                CHANGE_PASSWORD -> R.string.change_password
                CHANGE_PIN -> R.string.change_pin
                SHOW_SEED -> R.string.show_seed
                CJDNS_INFO -> R.string.cjdns_info
                WALLET_INFO -> R.string.wallet_info
                DATA_CONSENT -> R.string.data_consent
            }

        val iconResId: Int
            get() = when (this) {
                SHOW_SEED -> R.drawable.ic_arrow_down
                else -> R.drawable.ic_arrow_right
            }
    }
}

fun menuAdapterDelegate(
    onItemClick: (MenuItem) -> Unit,
) = adapterDelegateViewBinding<MenuItem, DisplayableItem, ItemMenuBinding>(
    { layoutInflater, root -> ItemMenuBinding.inflate(layoutInflater, root, false) }
) {

    bind {
        with(binding) {
            titleLabel.apply {
                setText(item.type.titleResId)
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, item.type.iconResId, 0)
                setOnClickListener {
                    onItemClick(item)
                }
            }

            divider.isVisible = item.showDivider
        }
    }
}
