package com.pkt.core.extensions

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.pkt.core.R
import com.pkt.core.presentation.common.state.event.CommonEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import java.lang.ref.WeakReference

fun Fragment.scrollWhenOpenKeyboard(scrollView: NestedScrollView) {
    KeyboardVisibilityEvent.setEventListener(requireActivity(), viewLifecycleOwner) { isOpen ->
        if (isOpen) {
            ((view?.findFocus() as? TextInputEditText)?.parent?.parent as? View)?.top?.let { top ->
                scrollView.post {
                    scrollView.smoothScrollTo(0, top)
                }
            }
        }
    }
}

fun Fragment.clearFocus() {
    view?.findFocus()?.let {
        it.clearFocus()
        UIUtil.hideKeyboard(it.context, it)
    }
}

@SuppressLint("InflateParams")
fun Fragment.showNotification(@StringRes titleResId: Int, type: CommonEvent.Notification.Type) {
    val rootView = (requireActivity().findViewById(android.R.id.content) as? FrameLayout) ?: return

    rootView.findViewById<View>(R.id.view_notification_id)?.let {
        rootView.removeView(it)
    }

    val backgroundColor = requireContext().getColorByAttribute(
        when (type) {
            CommonEvent.Notification.Type.SUCCESS -> R.attr.colorSuccess
            CommonEvent.Notification.Type.FAILURE -> androidx.appcompat.R.attr.colorError
        }
    )

    val notificationView = layoutInflater.inflate(R.layout.view_notification, null).apply {
        this.id = R.id.view_notification_id

        findViewById<TextView>(R.id.textLabel).apply {
            setText(titleResId)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                when (type) {
                    CommonEvent.Notification.Type.SUCCESS -> R.drawable.ic_notification_success
                    CommonEvent.Notification.Type.FAILURE -> R.drawable.ic_notification_failure
                },
                0, 0, 0
            )
        }

        backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    rootView.addView(notificationView, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

    val rootViewWeakRef = WeakReference(rootView)
    val notificationViewWeakRef = WeakReference(notificationView)
    rootView.postDelayed({
        if ((rootViewWeakRef.get()?.indexOfChild(notificationViewWeakRef.get()) ?: -1) > -1) {
            rootViewWeakRef.get()?.removeView(notificationViewWeakRef.get())

            rootViewWeakRef.get()?.context?.getColorByAttribute(android.R.attr.windowBackground)?.let { color ->
                setStatusBarColor(color)
            }
        }
    }, 2000)

    setStatusBarColor(backgroundColor)
}

fun Fragment.setStatusBarColor(@ColorInt color: Int) {
    val window = requireActivity().window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = color
}
