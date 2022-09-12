package com.pkt.core.presentation.common.state

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.pkt.core.R

fun ConstraintLayout.setProgress(isProgress: Boolean) {
    if (isProgress) {
        if (findViewById<View>(R.id.progressView) != null) {
            return
        }

        val progressView = LayoutInflater.from(context).inflate(R.layout.layout_progress, null)

        addView(progressView, ConstraintLayout.LayoutParams(0, 0))

        ConstraintSet().apply {
            clone(this)

            val toolbar = findViewById<View>(R.id.toolbar)
            if (toolbar == null) {
                connect(progressView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            } else {
                connect(progressView.id, ConstraintSet.TOP, toolbar.id, ConstraintSet.BOTTOM)
            }

            connect(progressView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            connect(progressView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(progressView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            applyTo(this@setProgress)
        }
    } else {
        findViewById<View>(R.id.progressView)?.let { removeView(it) }
    }
}

fun ConstraintLayout.setError(
    isError: Boolean,
    @StringRes errorResId: Int = R.string.error_common,
    onRetryClick: () -> Unit,
) {
    if (isError) {
        if (findViewById<View>(R.id.errorView) != null) {
            return
        }

        val retryErrorView = LayoutInflater.from(context).inflate(R.layout.layout_error, null).apply {
            this.findViewById<TextView>(R.id.errorLabel).setText(errorResId)
            this.findViewById<Button>(R.id.retryButton).setOnClickListener { onRetryClick() }
        }

        addView(retryErrorView, ConstraintLayout.LayoutParams(0, 0))

        ConstraintSet().apply {
            clone(this)

            val toolbar = findViewById<View>(R.id.toolbar)
            if (toolbar == null) {
                connect(retryErrorView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            } else {
                connect(retryErrorView.id, ConstraintSet.TOP, toolbar.id, ConstraintSet.BOTTOM)
            }

            connect(retryErrorView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            connect(retryErrorView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(retryErrorView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            applyTo(this@setError)
        }
    } else {
        findViewById<View>(R.id.errorView)?.let { removeView(it) }
    }
}
