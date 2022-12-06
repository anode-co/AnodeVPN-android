package com.pkt.core.extensions

import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.internal.ToolbarUtils
import com.google.android.material.textfield.TextInputLayout
import com.pkt.core.util.color.GradientFactory
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil

fun View.toPx(dp: Int): Int = context.toPx(dp)

fun TextView.applyGradient() {
    paint.shader = GradientFactory.linearGradient(paint.measureText(text.toString()), textSize)
}

fun MaterialToolbar.applyGradient() {
    ToolbarUtils.getTitleTextView(this)?.applyGradient()
}

inline fun TextInputLayout.doOnActionDone(
    crossinline action: (view: TextInputLayout) -> Unit,
) = editText?.setOnEditorActionListener { _, actionId, _ ->
    when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
            action.invoke(this)
            true
        }
        else -> false
    }
}

inline fun TextInputLayout.doOnActionSearch(
    crossinline action: (view: TextInputLayout) -> Unit,
) = editText?.setOnEditorActionListener { _, actionId, _ ->
    when (actionId) {
        EditorInfo.IME_ACTION_SEARCH -> {
            action.invoke(this)
            true
        }
        else -> false
    }
}

inline fun TextInputLayout.doOnTextChanged(
    crossinline action: (text: String) -> Unit,
) = editText?.doAfterTextChanged {
    error = null
    isErrorEnabled = false
    action.invoke(it.toString())
}

fun TextInputLayout.clearFocusOnActionDone() {
    doOnActionDone {
        clearFocus()
        UIUtil.hideKeyboard(context, this)
    }
}

fun TextInputLayout.clearFocusOnActionSearch() {
    doOnActionSearch {
        clearFocus()
        UIUtil.hideKeyboard(context, this)
    }
}

fun TextInputLayout.setError(@StringRes errorResId: Int) {
    error = context.getString(errorResId)

    (findViewById<TextView>(com.google.android.material.R.id.textinput_error)?.parent?.parent as? ViewGroup)?.apply {
        updatePaddingRelative(start = 0, end = 0)
    }

    editText?.let {
        it.requestFocus()
        UIUtil.showKeyboard(context, it)
    }
}

inline fun View.doOnClick(
    crossinline action: (view: View) -> Unit,
) = setOnClickListener {
    rootView?.findFocus()?.let {
        it.clearFocus()
        UIUtil.hideKeyboard(it.context, it)
    }
    action.invoke(this)
}

inline fun CheckBox.doOnCheckChanged(
    crossinline action: (isChecked: Boolean) -> Unit,
) = setOnCheckedChangeListener { _, isChecked ->
    /*rootView?.findFocus()?.let {
        it.clearFocus()
        UIUtil.hideKeyboard(it.context, it)
    }*/
    action.invoke(isChecked)
}

fun EditText.showKeyboard() {
    requestFocus()
    setSelection(text.length)
    UIUtil.showKeyboard(context, this)
}

fun TextInputLayout.showKeyboardDelayed() {
    editText?.showKeyboardDelayed()
}

fun EditText.showKeyboardDelayed() {
    postDelayed({
        requestFocus()
        setSelection(text.length)
        UIUtil.showKeyboard(context, this)
    }, 100)
}

fun EditText.hideKeyboard() {
    clearFocus()
    UIUtil.hideKeyboard(context, this)
}
