package com.example.eventmanagement.domain.usecase.dashboard

import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.domain.model.Event
import javax.inject.Inject

class GetNextUpcomingEventUseCase @Inject constructor(
    private val clock: AppClock
) {
    operator fun invoke(events: List<Event>): Event? {
        val now = clock.now()
        return events
            .asSequence()
            .filter { !DateFormatter.isInPast(it.dateTime, now) }
            .minByOrNull { it.dateTime }
    }
}
