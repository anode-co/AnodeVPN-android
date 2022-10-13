package com.pkt.core.presentation.common.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.ViewStrengthBinding
import com.pkt.core.extensions.getColorByAttribute
import com.ybs.passwordstrengthmeter.PasswordStrength

class StrengthView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {

    private val noneColor by lazy { ColorStateList.valueOf(ContextCompat.getColor(context, R.color.stroke1)) }
    private val weakColor by lazy { ColorStateList.valueOf(context.getColorByAttribute(androidx.appcompat.R.attr.colorError)) }
    private val mediumColor by lazy { ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorProgress)) }
    private val goodColor by lazy { ColorStateList.valueOf(context.getColorByAttribute(R.attr.colorSuccess)) }

    var strength: PasswordStrength = PasswordStrength.WEAK
        set(value) {
            field = value

            with(viewBinding) {
                when (value) {
                    PasswordStrength.WEAK -> {
                        weakView.backgroundTintList = weakColor
                        mediumView.backgroundTintList = noneColor
                        goodView.backgroundTintList = noneColor
                        perfectView.backgroundTintList = noneColor
                        valueLabel.apply {
                            setText(R.string.weak)
                            setTextColor(weakColor)
                        }
                    }

                    PasswordStrength.MEDIUM -> {
                        weakView.backgroundTintList = mediumColor
                        mediumView.backgroundTintList = mediumColor
                        goodView.backgroundTintList = noneColor
                        perfectView.backgroundTintList = noneColor
                        valueLabel.apply {
                            setText(R.string.medium)
                            setTextColor(mediumColor)
                        }
                    }

                    PasswordStrength.STRONG -> {
                        weakView.backgroundTintList = goodColor
                        mediumView.backgroundTintList = goodColor
                        goodView.backgroundTintList = goodColor
                        perfectView.backgroundTintList = noneColor
                        valueLabel.apply {
                            setText(R.string.good)
                            setTextColor(goodColor)
                        }
                    }

                    PasswordStrength.VERY_STRONG -> {
                        weakView.backgroundTintList = goodColor
                        mediumView.backgroundTintList = goodColor
                        goodView.backgroundTintList = goodColor
                        perfectView.backgroundTintList = goodColor
                        valueLabel.apply {
                            setText(R.string.perfect)
                            setTextColor(goodColor)
                        }
                    }
                }
            }
        }

    init {
        inflate(context, R.layout.view_strength, this)
    }

    private val viewBinding by viewBinding(ViewStrengthBinding::bind)
}
