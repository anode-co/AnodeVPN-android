package com.pkt.core.presentation.main.wallet

import androidx.annotation.StringRes
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.pkt.core.R
import com.pkt.core.databinding.*
import com.pkt.core.extensions.*
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.main.wallet.transaction.TransactionType
import java.time.LocalDateTime

class LoadingItem : DisplayableItem {
    override fun getItemId(): String = "LOADING_ITEM"
    override fun getItemHash(): String = hashCode().toString()
}

fun loadingAdapterDelegate() =
    adapterDelegateViewBinding<LoadingItem, DisplayableItem, ItemLoadingBinding>(
        { layoutInflater, root -> ItemLoadingBinding.inflate(layoutInflater, root, false) }
    ) {}

class ErrorItem : DisplayableItem {
    override fun getItemId(): String = "ERROR_ITEM"
    override fun getItemHash(): String = hashCode().toString()
}

fun errorAdapterDelegate(
    onRetryClick: (ErrorItem) -> Unit,
) =
    adapterDelegateViewBinding<ErrorItem, DisplayableItem, ItemErrorBinding>(
        { layoutInflater, root -> ItemErrorBinding.inflate(layoutInflater, root, false) }
    ) {
        binding.retryButton.setOnClickListener {
            onRetryClick(item)
        }
    }

class EmptyItem : DisplayableItem {
    override fun getItemId(): String = "EMPTY_ITEM"
    override fun getItemHash(): String = hashCode().toString()
}

fun emptyAdapterDelegate() =
    adapterDelegateViewBinding<EmptyItem, DisplayableItem, ItemEmptyBinding>(
        { layoutInflater, root -> ItemEmptyBinding.inflate(layoutInflater, root, false) }
    ) {}

data class FooterItem(
    @StringRes val textResId: Int = R.string.these_are_all_your_transactions,
) : DisplayableItem {
    override fun getItemId(): String = "FOOTER_ITEM"
    override fun getItemHash(): String = hashCode().toString()
}

fun footerAdapterDelegate() =
    adapterDelegateViewBinding<FooterItem, DisplayableItem, ItemFooterBinding>(
        { layoutInflater, root -> ItemFooterBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            binding.root.setText(item.textResId)
        }
    }

data class DateItem(
    val date: LocalDateTime,
) : DisplayableItem {
    override fun getItemId(): String = date.toString()
    override fun getItemHash(): String = hashCode().toString()
}

fun dateAdapterDelegate() =
    adapterDelegateViewBinding<DateItem, DisplayableItem, ItemDateBinding>(
        { layoutInflater, root -> ItemDateBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            binding.root.text = item.date.formatDateShort()
        }
    }

data class TransactionItem(
    val id: String,
    val type: TransactionType,
    val time: LocalDateTime,
    val amountPkt: String,
    val amountUsd: String,
    val transactionId: String,
    val addresses: List<String>,
    val blockNumber: Int,
    val confirmations: Int,
) : DisplayableItem {
    override fun getItemId(): String = id
    override fun getItemHash(): String = hashCode().toString()

}

fun transactionAdapterDelegate(
    onItemClick: (TransactionItem) -> Unit
) =
    adapterDelegateViewBinding<TransactionItem, DisplayableItem, ItemTransactionBinding>(
        { layoutInflater, root -> ItemTransactionBinding.inflate(layoutInflater, root, false) }
    ) {
        bind {
            with(binding) {
                when (item.type) {
                    TransactionType.SENT -> {
                        iconImage.setImageResource(R.drawable.ic_transaction_sent)
                        if (item.confirmations == 0) {
                            titleLabel.setText(R.string.sending_pkt_unconfirmed)
                            amountPktLabel.setTextColor(context.getColorByAttribute(R.attr.colorProgress))
                        } else {
                            titleLabel.setText(R.string.sent_pkt)
                            amountPktLabel.setTextColor(context.getColorByAttribute(android.R.attr.textColorPrimary))
                        }
                        amountPktLabel.text = "-${item.amountPkt}"
                        amountUsdLabel.text = item.amountUsd.formatUsd()
                    }

                    TransactionType.RECEIVE -> {
                        iconImage.setImageResource(R.drawable.ic_transaction_received)
                        titleLabel.setText(R.string.received_pkt)
                        amountPktLabel.setTextColor(context.getColorByAttribute(R.attr.colorSuccess))
                        amountPktLabel.text = item.amountPkt
                        amountUsdLabel.text = item.amountUsd.formatUsd()
                    }
                }

                timeLabel.text = item.time.formatTime()
                amountUsdLabel.text = item.amountUsd.formatUsd()

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }
