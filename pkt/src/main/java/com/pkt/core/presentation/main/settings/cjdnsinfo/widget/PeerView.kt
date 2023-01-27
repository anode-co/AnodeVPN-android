package com.pkt.core.presentation.main.settings.cjdnsinfo.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.domain.dto.CjdnsPeer
import com.pkt.core.R
import com.pkt.core.databinding.ItemKeyValueHorizontalBinding
import com.pkt.core.databinding.ViewPeerBinding
import com.pkt.core.extensions.firstLetterUppercase
import com.pkt.core.extensions.formatBytes
import com.pkt.core.extensions.getColorByAttribute
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.adapter.delegate.KeyValueHorizontalItem
import com.pkt.core.presentation.common.adapter.delegate.keyValueHorizontalAdapterDelegate

class PeerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val adapter = AsyncListDifferAdapter(
        keyValueHorizontalAdapterDelegate(),
        statusAdapterDelegate(),
    )

    var expanded: Boolean = true
        set(value) {
            if (field != value) {
                onExpandedStateChanged?.invoke(value)
            }

            field = value

            with(viewBinding) {
                titleLabel.apply {
                    setTextAppearance(
                        if (expanded) R.style.TextAppearance_App_14_SemiBold else R.style.TextAppearance_App_14_Regular
                    )
                    setTextColor(
                        context.getColorByAttribute(
                            if (expanded) android.R.attr.textColorPrimary else android.R.attr.colorPrimary
                        )
                    )
                }

                toggleButton.apply {
                    imageTintList = ColorStateList.valueOf(
                        context.getColorByAttribute(
                            if (expanded) android.R.attr.textColorPrimary else android.R.attr.colorPrimary
                        )
                    )
                    rotation = if (expanded) 0f else 180f
                }

                recyclerView.isVisible = expanded
            }
        }

    var onExpandedStateChanged: ((Boolean) -> Unit)? = null

    init {
        inflate(context, R.layout.view_peer, this)

        orientation = VERTICAL
    }

    private val viewBinding by viewBinding(ViewPeerBinding::bind)

    fun setup(peer: CjdnsPeer) {
        with(viewBinding) {
            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter

            toggleButton.setOnClickListener {
                toggle()
            }
            headerLayout.setOnClickListener {
                toggle()
            }

            val statusValue = peer.status.statusValue().replace("\"", "")
            val bytesInValue = "${peer.bytesIn.formatBytes(context)}/${context.getString(R.string.per_second)}"
            val bytesOutValue = "${peer.bytesOut.formatBytes(context)}/${context.getString(R.string.per_second)}"

            titleLabel.text = peer.ipv4
            statusLabel.text = "$statusValue - $bytesInValue / $bytesOutValue"
            var noise = "Legacy"
            if (peer.noiseProto == 1) {
                noise = "Noise"
            }
            adapter.items = listOf(
                KeyValueHorizontalItem(R.string.ip, "${peer.ipv4}:${peer.port}"),
                KeyValueHorizontalItem(R.string.key, peer.key.replace("\"", "")),
                StatusItem(statusValue, statusValue.statusColorResId()),
                KeyValueHorizontalItem(R.string.key_in, bytesInValue),
                KeyValueHorizontalItem(R.string.out, bytesOutValue),
                KeyValueHorizontalItem(R.string.loss, peer.bytesLost.toString()),
                KeyValueHorizontalItem(R.string.protocol, noise),
                KeyValueHorizontalItem(R.string.cjdns_ip, peer.cjdnsIp),
            )
        }
    }

    private fun toggle() {
        expanded = !expanded
    }

    private fun String.statusValue(): String = when (uppercase()) {
        "INIT" -> context.getString(R.string.no_communication_yet)
        "SENT_HELLO", "RECEIVED_HELLO", "SENT_KEY", "RECEIVED_KEY" -> context.getString(R.string.handshaking)
        "ESTABLISHED" -> context.getString(R.string.established)
        "UNRESPONSIVE" -> context.getString(R.string.unresponsive)
        "UNAUTHENTICATED" -> context.getString(R.string.unauthenticated)
        "INCOMPATIBLE" -> context.getString(R.string.incompatible)
        else -> firstLetterUppercase().replace('_', ' ')
    }

    @ColorRes
    private fun String.statusColorResId(): Int = when (uppercase()) {
        //"INIT" -> R.color.yellow
        //"SENT_HELLO", "RECEIVED_HELLO", "SENT_KEY", "RECEIVED_KEY" -> R.color.yellow
        "ESTABLISHED" -> R.color.green
        "UNRESPONSIVE" -> R.color.red
        //"UNAUTHENTICATED" -> R.color.red
        //"INCOMPATIBLE" -> R.color.red
        else -> R.color.yellow
    }

    private data class StatusItem(
        val value: String,
        @ColorRes val colorResId: Int,
    ) : DisplayableItem {
        override fun getItemId(): String = value
        override fun getItemHash(): String = hashCode().toString()
    }

    private fun statusAdapterDelegate() =
        adapterDelegateViewBinding<StatusItem, DisplayableItem, ItemKeyValueHorizontalBinding>(
            { layoutInflater, root -> ItemKeyValueHorizontalBinding.inflate(layoutInflater, root, false) }
        ) {
            bind {
                with(binding) {
                    keyLabel.setText(R.string.status)

                    valueLabel.apply {
                        text = item.value
                        setTextColor(ContextCompat.getColor(context, item.colorResId))
                    }
                }
            }
        }
}
