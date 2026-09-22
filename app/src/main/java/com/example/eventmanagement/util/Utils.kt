package com.example.eventmanagement.util

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updatePadding
import com.example.eventmanagement.R
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

    fun format(timestamp: Timestamp): String = format(timestamp.toDate())

    fun format(date: Date): String = formatter("EEE, dd MMM yyyy · hh:mm a").format(date)

    fun formatDate(date: Date): String = formatter("dd MMM yyyy").format(date)

    fun formatTime(date: Date): String = formatter("hh:mm a").format(date)

    fun monthKey(timestamp: Timestamp): String =
        formatter("MMM yyyy").format(timestamp.toDate())

    fun isInPast(date: Date): Boolean = date.before(Date())

    fun dayOfMonth(date: Date): String = formatter("dd").format(date)

    fun monthShort(date: Date): String =
        formatter("MMM").format(date).uppercase(Locale.getDefault())

    fun weekdayShort(date: Date): String = formatter("EEE").format(date)

    fun todayLong(): String = formatter("EEEE, dd MMMM").format(Date())

    fun daysUntil(date: Date): Int {
        val from = startOfDay().time
        val to = startOfDay(date).time
        return ((to - from) / MILLIS_PER_DAY).toInt()
    }

    fun startOfDay(date: Date = Date()): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}

object ThemePrefs {
    private const val PREFS = "theme_prefs"
    private const val KEY_MODE = "night_mode"

    fun applySavedTheme(context: Context) {
        val mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun setNightMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MODE, mode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun currentMode(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun isDark(context: Context): Boolean {
        return when (currentMode(context)) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}

fun View.applySystemBarInsets(includeBottom: Boolean = true) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            initialLeft + bars.left,
            initialTop + bars.top,
            initialRight + bars.right,
            initialBottom + if (includeBottom) bars.bottom else 0
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyTopInsetAsPadding() {
    val initialTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(top = initialTop + bars.top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyBottomInsetAsPadding() {
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(bottom = initialBottom + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

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

fun View.startSkeletonPulse() {
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.skeleton_pulse))
}

fun View.stopSkeletonPulse() {
    clearAnimation()
}

object Validators {
    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email is required."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Enter a valid email address."
        }
        return null
    }

    fun validatePassword(password: String, minLength: Int = 6): String? {
        if (password.isBlank()) return "Password is required."
        if (password.length < minLength) return "Password must be at least $minLength characters."
        return null
    }

    fun validateEventTitle(title: String): String? {
        if (title.isBlank()) return "Title is required."
        return null
    }
}
