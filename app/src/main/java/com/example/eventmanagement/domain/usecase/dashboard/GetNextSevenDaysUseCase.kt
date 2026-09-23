package com.example.eventmanagement.domain.usecase.dashboard

import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.domain.model.DayLoad
import com.example.eventmanagement.domain.model.Event
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetNextSevenDaysUseCase @Inject constructor(
    private val clock: AppClock
) {
    operator fun invoke(events: List<Event>): List<DayLoad> {
        val now = clock.now()
        val counts = IntArray(7)
        events.forEach { event ->
            val offset = DateFormatter.daysUntil(event.dateTime, now)
            if (offset in 0..6) counts[offset]++
        }
        return (0..6).map { offset ->
            val day = now.plus(offset.toLong(), ChronoUnit.DAYS)
            DayLoad(
                label = DateFormatter.weekdayShort(day),
                dayOfMonth = DateFormatter.dayOfMonth(day),
                count = counts[offset],
                isToday = offset == 0
            )
        }
    }
}
