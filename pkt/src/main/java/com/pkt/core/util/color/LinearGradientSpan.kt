package com.pkt.core.util.color

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

class LinearGradientSpan(private val text: String) : CharacterStyle(), UpdateAppearance {

    override fun updateDrawState(tp: TextPaint) {
        tp.shader = GradientFactory.linearGradient(tp.measureText(text), 0f)
    }
}
