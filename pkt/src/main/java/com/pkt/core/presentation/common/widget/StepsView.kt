package com.pkt.core.presentation.common.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.ViewStepsBinding
import com.pkt.core.extensions.getColorByAttribute
import com.pkt.core.presentation.createwallet.CreateWalletMode

class StepsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {

    private val normalColor by lazy { ColorStateList.valueOf(ContextCompat.getColor(context, R.color.stroke1)) }
    private val activeColor by lazy { ColorStateList.valueOf(context.getColorByAttribute(android.R.attr.colorPrimary)) }

    private val dots by lazy {
        listOf(
            viewBinding.dot1View,
            viewBinding.dot2View,
            viewBinding.dot3View,
            viewBinding.dot4View,
            viewBinding.dot5View
        )
    }

    private val lines by lazy {
        listOf(
            viewBinding.line1View,
            viewBinding.line2View,
            viewBinding.line3View,
            viewBinding.line4View,
        )
    }

    var currentStep: Int = 0
        set(value) {
            field = value

            dots.forEachIndexed { index, view ->
                view.backgroundTintList = if (value >= index) activeColor else normalColor
            }
            lines.forEachIndexed { index, view ->
                view.backgroundTintList = if (value > index) activeColor else normalColor
            }
        }

    var mode: CreateWalletMode = CreateWalletMode.CREATE
        set(value) {
            field = value

            when (value) {
                CreateWalletMode.CREATE -> {
                    dots.forEach { it.isVisible = true }
                    lines.forEach { it.isVisible = true }
                }
                CreateWalletMode.RECOVER -> {
                    dots.forEachIndexed { index, view -> view.isVisible = index < dots.size - 1 }
                    lines.forEachIndexed { index, view -> view.isVisible = index < lines.size - 1 }
                }
            }
        }

    init {
        inflate(context, R.layout.view_steps, this)
    }

    private val viewBinding by viewBinding(ViewStepsBinding::bind)
}
