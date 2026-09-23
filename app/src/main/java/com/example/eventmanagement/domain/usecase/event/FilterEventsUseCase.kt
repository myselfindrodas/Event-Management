package com.example.eventmanagement.domain.usecase.event

import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.model.EventFilter
import javax.inject.Inject

class FilterEventsUseCase @Inject constructor(
    private val clock: AppClock
) {
    operator fun invoke(
        events: List<Event>,
        query: String,
        filter: EventFilter
    ): List<Event> {
        val now = clock.now()
        val normalized = query.trim().lowercase()
        return events.filter { event ->
            val matchesQuery = normalized.isEmpty() ||
                event.title.lowercase().contains(normalized) ||
                event.description.lowercase().contains(normalized) ||
                event.location.lowercase().contains(normalized)

            val matchesFilter = when (filter) {
                EventFilter.ALL -> true
                EventFilter.UPCOMING -> !DateFormatter.isInPast(event.dateTime, now)
                EventFilter.PAST -> DateFormatter.isInPast(event.dateTime, now)
            }
            matchesQuery && matchesFilter
        }
    }
}
