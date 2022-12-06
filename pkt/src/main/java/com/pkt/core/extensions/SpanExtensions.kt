package com.pkt.core.extensions

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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

fun String.highlightText(@ColorInt color: Int, text: String) = SpannableString(this).apply {
    highlightText(this, color, text)
}

private fun highlightText(spannable: Spannable, @ColorInt color: Int, text: String) {
    val span = ForegroundColorSpan(color)
    val start = spannable.indexOf(text, ignoreCase = true)
    if (start > -1) {
        spannable.setSpan(span, start, start + text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }
}
