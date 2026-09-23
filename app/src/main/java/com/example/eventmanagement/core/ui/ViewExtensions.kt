package com.example.eventmanagement.core.ui

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale

fun TextView.animateCount(target: Int, durationMs: Long = 680L) {
    fun format(value: Int) = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

    val start = runCatching {
        NumberFormat.getIntegerInstance(Locale.getDefault()).parse(text.toString())?.toInt()
    }.getOrNull() ?: 0

    if (start == target) {
        text = format(target)
        return
    }
    ValueAnimator.ofInt(start, target).apply {
        duration = durationMs
        interpolator = DecelerateInterpolator()
        addUpdateListener { text = format(it.animatedValue as Int) }
        start()
    }
}
