package com.example.eventmanagement.domain.usecase.dashboard

import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.model.EventStats
import javax.inject.Inject

class GetEventStatsUseCase @Inject constructor(
    private val clock: AppClock
) {
    operator fun invoke(events: List<Event>): EventStats {
        val now = clock.now()
        val upcoming = events.count { !DateFormatter.isInPast(it.dateTime, now) }
        val past = events.count { DateFormatter.isInPast(it.dateTime, now) }
        val thisWeek = events.count {
            val days = DateFormatter.daysUntil(it.dateTime, now)
            days in 0..6
        }
        val completion = if (events.isEmpty()) 0 else (past * 100) / events.size
        return EventStats(
            total = events.size,
            upcoming = upcoming,
            past = past,
            thisWeek = thisWeek,
            completionPercent = completion
        )
    }
}
