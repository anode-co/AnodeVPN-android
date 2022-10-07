package com.pkt.app.util.color.span

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.ColorInt

open class CustomClickableSpan(
    private val onClickListener: () -> Unit,
    @ColorInt private val linkColor: Int? = null,
    private val isUnderlineText: Boolean = true,
) : ClickableSpan() {

    override fun updateDrawState(ds: TextPaint) {
        ds.color = linkColor ?: ds.linkColor
        ds.isUnderlineText = isUnderlineText
    }

    override fun onClick(widget: View) {
        onClickListener.invoke()
    }
}
