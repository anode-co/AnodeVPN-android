package com.pkt.core.presentation.common.adapter.delegate

import androidx.annotation.StringRes
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.databinding.ItemKeyValueHorizontalBinding
import com.pkt.core.presentation.common.adapter.DisplayableItem

data class KeyValueHorizontalItem(
    @StringRes val keyResId: Int,
    val value: String?,
) : DisplayableItem {
    override fun getItemId(): String = keyResId.toString()
    override fun getItemHash(): String = hashCode().toString()
}

fun keyValueHorizontalAdapterDelegate() =
    adapterDelegateViewBinding<KeyValueHorizontalItem, DisplayableItem, ItemKeyValueHorizontalBinding>(
        { layoutInflater, root -> ItemKeyValueHorizontalBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                keyLabel.setText(item.keyResId)
                valueLabel.text = item.value
            }
        }
    }
