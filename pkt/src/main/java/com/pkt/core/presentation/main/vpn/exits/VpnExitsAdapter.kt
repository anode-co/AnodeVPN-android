package com.pkt.core.presentation.main.vpn.exits

import androidx.core.content.ContextCompat
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.R
import com.pkt.core.databinding.ItemVpnExitBinding
import com.pkt.core.presentation.common.adapter.DisplayableItem

data class VpnExitItem(
    val name: String,
    val countryFlag: String,
    val countryName: String,
    val countryCode: String,
    val isConnected: Boolean,
    val publicKey: String,
    val isActive: Boolean,
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
            if (!item.isActive) {
                nameLabel.setTextColor(ContextCompat.getColor(context, R.color.text1_50))
            } else {
                nameLabel.setTextColor(ContextCompat.getColor(context, R.color.text1))
            }
            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
