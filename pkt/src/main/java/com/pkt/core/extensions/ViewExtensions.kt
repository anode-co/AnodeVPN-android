package com.pkt.core.extensions

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.internal.ToolbarUtils
import com.pkt.core.util.color.GradientFactory

fun View.toPx(dp: Int): Int = context.toPx(dp.toFloat()).toInt()

fun TextView.applyGradient() {
    paint.shader = GradientFactory.linearGradient(paint.measureText(text.toString()), textSize)
}

@SuppressLint("RestrictedApi")
fun MaterialToolbar.applyGradient() {
    ToolbarUtils.getTitleTextView(this)?.applyGradient()
}
