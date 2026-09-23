package com.example.eventmanagement.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateFormatter {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun format(instant: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy · hh:mm a", locale)
            .withZone(zone)
            .format(instant)

    fun formatDate(instant: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("dd MMM yyyy", locale)
            .withZone(zone)
            .format(instant)

    fun formatTime(instant: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("hh:mm a", locale)
            .withZone(zone)
            .format(instant)

    fun monthKey(instant: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("MMM yyyy", locale)
            .withZone(zone)
            .format(instant)

    fun dayOfMonth(instant: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("dd", locale).withZone(zone).format(instant)

    fun monthShort(instant: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("MMM", locale)
            .withZone(zone)
            .format(instant)
            .uppercase(locale)

    fun weekdayShort(instant: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEE", locale).withZone(zone).format(instant)

    fun todayLong(now: Instant, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEEE, dd MMMM", locale)
            .withZone(zone)
            .format(now)

    fun isInPast(instant: Instant, now: Instant): Boolean = instant.isBefore(now)

    fun daysUntil(instant: Instant, now: Instant): Int {
        val from = LocalDate.ofInstant(now, zone)
        val to = LocalDate.ofInstant(instant, zone)
        return ChronoUnit.DAYS.between(from, to).toInt()
    }

    fun hourOfDay(now: Instant): Int = now.atZone(zone).hour
}
