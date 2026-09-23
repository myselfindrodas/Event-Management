package com.example.eventmanagement.domain.usecase.dashboard

import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.domain.model.DashboardData
import com.example.eventmanagement.domain.model.Event
import javax.inject.Inject

class BuildDashboardDataUseCase @Inject constructor(
    private val getEventStatsUseCase: GetEventStatsUseCase,
    private val getNextUpcomingEventUseCase: GetNextUpcomingEventUseCase,
    private val getMonthlyEventCountsUseCase: GetMonthlyEventCountsUseCase,
    private val getNextSevenDaysUseCase: GetNextSevenDaysUseCase,
    private val clock: AppClock
) {
    operator fun invoke(events: List<Event>): DashboardData {
        val now = clock.now()
        val next = getNextUpcomingEventUseCase(events)
        val monthlyCounts = getMonthlyEventCountsUseCase(events)
        return DashboardData(
            stats = getEventStatsUseCase(events),
            nextEvent = next,
            nextEventDays = next?.let { DateFormatter.daysUntil(it.dateTime, now) } ?: 0,
            weekLoad = getNextSevenDaysUseCase(events),
            busiestMonth = monthlyCounts.maxByOrNull { it.value }?.key,
            monthlyAverage = if (monthlyCounts.isEmpty()) {
                0.0
            } else {
                events.size.toDouble() / monthlyCounts.size
            },
            monthlyCounts = monthlyCounts
        )
    }
}
