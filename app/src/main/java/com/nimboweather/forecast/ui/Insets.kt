package com.nimboweather.forecast.ui

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * System-bar inset helpers. targetSdk 35 forces edge-to-edge on Android 15+, so
 * content otherwise draws under the status bar (top) and gesture/nav bar (bottom).
 * These apply the system-bar + display-cutout insets to the requested edges while
 * preserving whatever padding/margins the view already declares in XML.
 */

private fun WindowInsetsCompat.bars() = getInsets(
    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
)

/** Pad [this] by the insets on the chosen edges, on top of the view's existing padding. */
fun View.applySystemBarInsets(
    top: Boolean = false, bottom: Boolean = false,
    left: Boolean = false, right: Boolean = false,
) {
    val l = paddingLeft; val t = paddingTop; val r = paddingRight; val b = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.bars()
        v.updatePadding(
            left = l + (if (left) bars.left else 0),
            top = t + (if (top) bars.top else 0),
            right = r + (if (right) bars.right else 0),
            bottom = b + (if (bottom) bars.bottom else 0),
        )
        insets
    }
}

/**
 * Offset [this] via margins by the insets on the chosen edges — for overlays where
 * padding would shrink match_parent children (e.g. the fullscreen-ad close button).
 * Requires the view's layout params to be [ViewGroup.MarginLayoutParams].
 */
fun View.applySystemBarMargins(
    top: Boolean = false, bottom: Boolean = false,
    left: Boolean = false, right: Boolean = false,
) {
    val lp = layoutParams as ViewGroup.MarginLayoutParams
    val l = lp.leftMargin; val t = lp.topMargin; val r = lp.rightMargin; val b = lp.bottomMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.bars()
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = l + (if (left) bars.left else 0)
            topMargin = t + (if (top) bars.top else 0)
            rightMargin = r + (if (right) bars.right else 0)
            bottomMargin = b + (if (bottom) bars.bottom else 0)
        }
        insets
    }
}

/** The single child the activity's content view hosts — i.e. the inflated layout root. */
fun AppCompatActivity.contentRootChild(): View =
    findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
