package com.pkt.core.presentation.main.vpn.exits

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.R
import com.pkt.core.databinding.ItemVpnExitBinding
import com.pkt.core.presentation.common.adapter.DisplayableItem
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

data class VpnExitItem(
    val name: String,
    val countryFlag: String,
    val countryName: String,
    val countryCode: String,
    val isConnected: Boolean,
    val publicKey: String,
    val isActive: Boolean,
    val isPremium: Boolean,
    val premiumEndTime: Long = 0L,
    val cost: Int = 0,
) : DisplayableItem {
    override fun getItemId(): String = name
    override fun getItemHash(): String = hashCode().toString()
}

@SuppressLint("SetTextI18n")
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
            premiumEndTime.visibility = android.view.View.GONE
            premiumImage.visibility = android.view.View.GONE
            if (item.isPremium) {
                premiumImage.visibility = android.view.View.VISIBLE
                if (item.premiumEndTime > System.currentTimeMillis()) {
                    val localDateTime = LocalDateTime.ofEpochSecond((item.premiumEndTime/1000), 0, ZoneOffset.systemDefault().rules.getOffset(Instant.now()) )
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    premiumEndTime.text = "${getString(R.string.premium_ends_short)} ${localDateTime.format(formatter)}"
                    premiumEndTime.visibility = android.view.View.VISIBLE
                }
            }
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
