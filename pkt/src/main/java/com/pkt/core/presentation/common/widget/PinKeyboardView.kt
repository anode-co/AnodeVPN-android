package com.pkt.core.presentation.common.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.ViewPinKeyboardBinding

class PinKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {

    var onKeyClick: ((PinKey) -> Unit)? = null
        set(value) {
            field = value

            viewBinding.key1Button.setOnClickListener { value?.invoke(PinKey.KEY_1) }
            viewBinding.key2Button.setOnClickListener { value?.invoke(PinKey.KEY_2) }
            viewBinding.key3Button.setOnClickListener { value?.invoke(PinKey.KEY_3) }
            viewBinding.key4Button.setOnClickListener { value?.invoke(PinKey.KEY_4) }
            viewBinding.key5Button.setOnClickListener { value?.invoke(PinKey.KEY_5) }
            viewBinding.key6Button.setOnClickListener { value?.invoke(PinKey.KEY_6) }
            viewBinding.key7Button.setOnClickListener { value?.invoke(PinKey.KEY_7) }
            viewBinding.key8Button.setOnClickListener { value?.invoke(PinKey.KEY_8) }
            viewBinding.key9Button.setOnClickListener { value?.invoke(PinKey.KEY_9) }
            viewBinding.key0Button.setOnClickListener { value?.invoke(PinKey.KEY_0) }
            viewBinding.keyLogoutButton.setOnClickListener { value?.invoke(PinKey.KEY_LOG_OUT) }
            viewBinding.keyApplyButton.setOnClickListener { value?.invoke(PinKey.KEY_APPLY) }
        }

    init {
        inflate(context, R.layout.view_pin_keyboard, this)
    }

    private val viewBinding by viewBinding(ViewPinKeyboardBinding::bind)

    enum class PinKey {
        KEY_1,
        KEY_2,
        KEY_3,
        KEY_4,
        KEY_5,
        KEY_6,
        KEY_7,
        KEY_8,
        KEY_9,
        KEY_0,
        KEY_LOG_OUT,
        KEY_APPLY,
    }
}
