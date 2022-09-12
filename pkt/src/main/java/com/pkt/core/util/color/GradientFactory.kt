package com.pkt.core.util.color

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader

object GradientFactory {

    fun linearGradient(x1: Float, y1: Float) =
        LinearGradient(
            0f,
            0f,
            x1,
            y1,
            intArrayOf(
                Color.parseColor("#D4D7E9"),
                Color.parseColor("#8EBED6"),
                Color.parseColor("#4BA7C4"),
            ),
            floatArrayOf(
                0f,
                0.6f,
                1f,
            ),
            Shader.TileMode.CLAMP
        )
}
