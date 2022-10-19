package com.pkt.core.presentation.main.vpn.exits

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.databinding.ItemVpnExitBinding
import com.pkt.core.presentation.common.adapter.DisplayableItem

data class VpnExitItem(
    val name: String,
    val countryFlag: String,
    val countryName: String,
    val isConnected: Boolean,
) : DisplayableItem {
    override fun getItemId(): String = name
    override fun getItemHash(): String = hashCode().toString()
}

fun vpnExitAdapterDelegate(
    onItemClick: (VpnExitItem) -> Unit,
) = adapterDelegateViewBinding<VpnExitItem, DisplayableItem, ItemVpnExitBinding>(
    { layoutInflater, root -> ItemVpnExitBinding.inflate(layoutInflater, root, false) }
) {
    bind {
        with(binding) {
            flagImage.text = item.countryFlag
            nameLabel.text = item.name
            countryLabel.text = item.countryName
            connectedImage.isActivated = item.isConnected

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
