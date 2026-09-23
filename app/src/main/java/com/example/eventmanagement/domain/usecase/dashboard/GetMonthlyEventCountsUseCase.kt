package com.example.eventmanagement.domain.usecase.dashboard

import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.domain.model.Event
import javax.inject.Inject

class GetMonthlyEventCountsUseCase @Inject constructor() {
    operator fun invoke(events: List<Event>): Map<String, Int> {
        return events
            .groupBy { DateFormatter.monthKey(it.dateTime) }
            .mapValues { it.value.size }
            .toSortedMap(
                compareBy { key ->
                    events.firstOrNull { DateFormatter.monthKey(it.dateTime) == key }
                        ?.dateTime
                        ?.toEpochMilli()
                        ?: 0L
                }
            )
    }

    fun busiestMonth(events: List<Event>): String? =
        invoke(events).maxByOrNull { it.value }?.key

    fun monthlyAverage(events: List<Event>): Double {
        val perMonth = invoke(events)
        if (perMonth.isEmpty()) return 0.0
        return events.size.toDouble() / perMonth.size
    }
}
