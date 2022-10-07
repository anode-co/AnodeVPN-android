package com.pkt.core.extensions

import android.text.Spannable
import androidx.annotation.ColorInt
import com.pkt.app.util.color.span.CustomClickableSpan

fun Spannable.setClickOnText(
    text: String,
    @ColorInt color: Int,
    isUnderlineText: Boolean,
    onClickListener: () -> Unit,
) {
    val span = CustomClickableSpan(onClickListener, color, isUnderlineText)
    val start = indexOf(text, ignoreCase = true)
    setSpan(span, start, start + text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
}
