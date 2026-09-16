package com.example.urwallet.core.designsystem

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.urwallet.R
import com.google.android.material.snackbar.Snackbar

object UrWalletFeedback {

    /**
     * Performs a light tactile click haptic vibration.
     */
    fun performClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Performs a confirmation / success haptic vibration.
     */
    fun performSuccess(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Performs a rejection / warning haptic vibration.
     */
    fun performWarning(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Shows a standardized Material 3 Success Snackbar with green accent and haptic feedback.
     */
    fun showSuccessSnackbar(
        view: View,
        message: String,
        anchorView: View? = null,
        duration: Int = Snackbar.LENGTH_SHORT
    ): Snackbar {
        performSuccess(view)
        return buildSnackbar(
            view = view,
            message = message,
            backgroundColor = ContextCompat.getColor(view.context, R.color.urwallet_income),
            textColor = ContextCompat.getColor(view.context, R.color.white),
            anchorView = anchorView,
            duration = duration
        ).also { it.show() }
    }

    /**
     * Shows a standardized Material 3 Error Snackbar with red accent and warning haptic feedback.
     */
    fun showErrorSnackbar(
        view: View,
        message: String,
        anchorView: View? = null,
        duration: Int = Snackbar.LENGTH_LONG
    ): Snackbar {
        performWarning(view)
        return buildSnackbar(
            view = view,
            message = message,
            backgroundColor = ContextCompat.getColor(view.context, R.color.urwallet_expense),
            textColor = ContextCompat.getColor(view.context, R.color.white),
            anchorView = anchorView,
            duration = duration
        ).also { it.show() }
    }

    /**
     * Shows a standardized Material 3 Info / Primary Snackbar.
     */
    fun showInfoSnackbar(
        view: View,
        message: String,
        anchorView: View? = null,
        duration: Int = Snackbar.LENGTH_SHORT
    ): Snackbar {
        performClick(view)
        return buildSnackbar(
            view = view,
            message = message,
            backgroundColor = ContextCompat.getColor(view.context, R.color.urwallet_primary),
            textColor = ContextCompat.getColor(view.context, R.color.white),
            anchorView = anchorView,
            duration = duration
        ).also { it.show() }
    }

    private fun buildSnackbar(
        view: View,
        message: String,
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        anchorView: View? = null,
        duration: Int = Snackbar.LENGTH_SHORT
    ): Snackbar {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(backgroundColor)
        snackbar.setTextColor(textColor)
        if (anchorView != null) {
            snackbar.anchorView = anchorView
        }
        return snackbar
    }
}

// Extension helpers on View
fun View.performHapticClick() = UrWalletFeedback.performClick(this)
fun View.performHapticSuccess() = UrWalletFeedback.performSuccess(this)
fun View.performHapticWarning() = UrWalletFeedback.performWarning(this)

// Extension helpers on Fragment
fun Fragment.showSuccessSnackbar(message: String, anchorView: View? = null) {
    view?.let { UrWalletFeedback.showSuccessSnackbar(it, message, anchorView) }
}

fun Fragment.showErrorSnackbar(message: String, anchorView: View? = null) {
    view?.let { UrWalletFeedback.showErrorSnackbar(it, message, anchorView) }
}

fun Fragment.showInfoSnackbar(message: String, anchorView: View? = null) {
    view?.let { UrWalletFeedback.showInfoSnackbar(it, message, anchorView) }
}
