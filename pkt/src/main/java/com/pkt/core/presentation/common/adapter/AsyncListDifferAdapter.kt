package com.pkt.core.presentation.common.adapter

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter

class AsyncListDifferAdapter(
    vararg delegates: AdapterDelegate<List<DisplayableItem>>,
) : AsyncListDifferDelegationAdapter<DisplayableItem>(DiffItemCallback(), *delegates) {

    private class DiffItemCallback : DiffUtil.ItemCallback<DisplayableItem>() {

        override fun areItemsTheSame(oldItem: DisplayableItem, newItem: DisplayableItem): Boolean =
            oldItem.getItemId() == newItem.getItemId()

        override fun areContentsTheSame(oldItem: DisplayableItem, newItem: DisplayableItem): Boolean =
            oldItem.getItemHash() == newItem.getItemHash()
    }
}
