package com.pkt.core.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

@ColorInt
fun Context.getColorByAttribute(resId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(resId, typedValue, true)
    return ContextCompat.getColor(this, typedValue.resourceId)
}

fun Context.toPx(dp: Int): Int = toPx(dp.toFloat()).toInt()

fun Context.toPx(dp: Float): Float = dp * resources.displayMetrics.density

fun Context.copyToBuffer(@StringRes labelResId: Int, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(getString(labelResId), text)
    clipboard.setPrimaryClip(clip)
}
