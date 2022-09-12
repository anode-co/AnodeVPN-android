package com.pkt.core.presentation.main.settings.walletinfo

import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.R
import com.pkt.core.databinding.ItemConnectedServerBinding
import com.pkt.core.databinding.ItemConnectedServersBinding
import com.pkt.core.databinding.ItemKeyValueVerticalBinding
import com.pkt.core.extensions.applyGradient
import com.pkt.core.extensions.formatDateTime
import com.pkt.core.extensions.formatPkt
import com.pkt.core.presentation.common.adapter.DisplayableItem

interface ValueFormatter {
    fun format(params: List<Any>): String

    companion object {

        val DEFAULT: ValueFormatter = object : ValueFormatter {
            override fun format(params: List<Any>): String = params.first().toString()
        }

        val BALANCE: ValueFormatter = object : ValueFormatter {
            override fun format(params: List<Any>): String = (params.first() as Double).formatPkt()
        }

        val SYNC: ValueFormatter = object : ValueFormatter {
            override fun format(params: List<Any>): String = "${params[0]} / ${params[1].toString().formatDateTime()}"
        }
    }
}

data class KeyValueVerticalItem(
    @StringRes val keyResId: Int,
    val params: List<Any>,
    val formatter: ValueFormatter = ValueFormatter.DEFAULT,
) : DisplayableItem {

    constructor(
        @StringRes keyResId: Int,
        value: String,
        formatter: ValueFormatter = ValueFormatter.DEFAULT,
    ) : this(keyResId, listOf(value), formatter)

    override fun getItemId(): String = keyResId.toString()
    override fun getItemHash(): String = hashCode().toString()
}

fun keyValueVerticalAdapterDelegate() =
    adapterDelegateViewBinding<KeyValueVerticalItem, DisplayableItem, ItemKeyValueVerticalBinding>(
        { layoutInflater, root -> ItemKeyValueVerticalBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                keyLabel.setText(item.keyResId)
                valueLabel.text = item.formatter.format(item.params)
            }
        }
    }

data class ConnectedServersItem(
    val count: Int,
) : DisplayableItem {
    override fun getItemId(): String = "CONNECTED_SERVERS_ITEM"
    override fun getItemHash(): String = hashCode().toString()
}

fun connectedServersAdapterDelegate() =
    adapterDelegateViewBinding<ConnectedServersItem, DisplayableItem, ItemConnectedServersBinding>(
        { layoutInflater, root -> ItemConnectedServersBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                titleLabel.applyGradient()
                countLabel.text = item.count.toString()
            }
        }
    }

data class ConnectedServerItem(
    val address: String,
    val lastBlock: Int,
    val showDivider: Boolean,
) : DisplayableItem {
    override fun getItemId(): String = address
    override fun getItemHash(): String = hashCode().toString()
}

fun connectedServerAdapterDelegate() =
    adapterDelegateViewBinding<ConnectedServerItem, DisplayableItem, ItemConnectedServerBinding>(
        { layoutInflater, root -> ItemConnectedServerBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                addressLabel.text = item.address
                syncLabel.text = "%s %s".format(getString(R.string.sync), item.lastBlock)
                divider.isVisible = item.showDivider
            }
        }
    }
