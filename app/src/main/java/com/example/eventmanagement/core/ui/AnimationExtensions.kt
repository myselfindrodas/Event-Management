package com.example.eventmanagement.core.ui

import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.children
import com.example.eventmanagement.R

fun View.animateEntrance(delay: Long = 0L) {
    alpha = 0f
    translationY = 24f
    animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(delay)
        .setDuration(320L)
        .setInterpolator(DecelerateInterpolator(1.6f))
        .start()
}

fun ViewGroup.staggerChildren(stepMs: Long = 55L, startDelayMs: Long = 60L) {
    children.forEachIndexed { index, child ->
        child.animateEntrance(startDelayMs + index * stepMs)
    }
}

fun View.animatePop(delay: Long = 0L) {
    alpha = 0f
    scaleX = 0.92f
    scaleY = 0.92f
    animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setStartDelay(delay)
        .setDuration(420L)
        .setInterpolator(OvershootInterpolator(1.4f))
        .start()
}

fun View.startSkeletonPulse() {
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.skeleton_pulse))
}

fun View.stopSkeletonPulse() {
    clearAnimation()
}
