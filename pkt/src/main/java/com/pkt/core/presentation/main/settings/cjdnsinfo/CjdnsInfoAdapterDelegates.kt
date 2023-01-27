package com.pkt.core.presentation.main.settings.cjdnsinfo

import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.R
import com.pkt.domain.dto.CjdnsPeer
import com.pkt.core.databinding.ItemKeyValueClickableBinding
import com.pkt.core.databinding.ItemKeyValueHorizontalBinding
import com.pkt.core.databinding.ItemPeerBinding
import com.pkt.core.databinding.ItemTitleBinding
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.main.settings.cjdnsinfo.widget.PeerView

data class KeyValueClickableItem(
    @StringRes val keyResId: Int,
    @StringRes val valueResId: Int,
) : DisplayableItem {
    override fun getItemId(): String = keyResId.toString()
    override fun getItemHash(): String = hashCode().toString()
}

fun keyValueClickableAdapterDelegate(
    onItemClick: (KeyValueClickableItem) -> Unit,
) =
    adapterDelegateViewBinding<KeyValueClickableItem, DisplayableItem, ItemKeyValueClickableBinding>(
        { layoutInflater, root -> ItemKeyValueClickableBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                keyLabel.setText(item.keyResId)

                valueButton.apply {
                    setText(item.valueResId)
                    setOnClickListener {
                        onItemClick(item)
                    }
                }
            }
        }
    }

data class TitleItem(
    @StringRes val titleResId: Int,
) : DisplayableItem {
    override fun getItemId(): String = titleResId.toString()
    override fun getItemHash(): String = hashCode().toString()
}

data class StatusItem(
    val value: String
) : DisplayableItem {
    override fun getItemId(): String = value
    override fun getItemHash(): String = hashCode().toString()
}

fun titleAdapterDelegate() =
    adapterDelegateViewBinding<TitleItem, DisplayableItem, ItemTitleBinding>(
        { layoutInflater, root -> ItemTitleBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            binding.root.setText(item.titleResId)
        }
    }

data class PeerItem(
    val peer: CjdnsPeer,
    val showDivider: Boolean,
    var expanded: Boolean = false,
) : DisplayableItem {

    val id: String = "${peer.ipv4}:${peer.port}"

    override fun getItemId(): String = id
    override fun getItemHash(): String = hashCode().toString()
}


fun statusAdapterDelegate() =
    adapterDelegateViewBinding<StatusItem, DisplayableItem, ItemKeyValueHorizontalBinding>(
        { layoutInflater, root -> ItemKeyValueHorizontalBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                keyLabel.setText(R.string.status)

                valueLabel.apply {
                    text = item.value
                    when (item.value) {
                        "ESTABLISHED" -> setTextColor(ContextCompat.getColor(context, R.color.green))
                        "UNRESPONSIVE" -> setTextColor(ContextCompat.getColor(context, R.color.red))
                        else -> setTextColor(ContextCompat.getColor(context, R.color.yellow))
                    }

                }
            }
        }
    }

fun peerAdapterDelegate() =
    adapterDelegateViewBinding<PeerItem, DisplayableItem, ItemPeerBinding>(
        { layoutInflater, root -> ItemPeerBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                peerView.apply {
                    setup(item.peer)
                    expanded = item.expanded
                    onExpandedStateChanged = {
                        item.expanded = it
                    }
                }

                divider.isVisible = item.showDivider
            }
        }
    }
