package com.example.eventmanagement.domain.usecase.dashboard

import com.example.eventmanagement.core.time.FakeAppClock
import com.example.eventmanagement.domain.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class DashboardUseCasesTest {

    private lateinit var clock: FakeAppClock
    private lateinit var statsUseCase: GetEventStatsUseCase
    private lateinit var nextUseCase: GetNextUpcomingEventUseCase
    private lateinit var monthlyUseCase: GetMonthlyEventCountsUseCase
    private lateinit var weekUseCase: GetNextSevenDaysUseCase

    private val now = Instant.parse("2026-06-15T12:00:00Z")

    @Before
    fun setup() {
        clock = FakeAppClock(now)
        statsUseCase = GetEventStatsUseCase(clock)
        nextUseCase = GetNextUpcomingEventUseCase(clock)
        monthlyUseCase = GetMonthlyEventCountsUseCase()
        weekUseCase = GetNextSevenDaysUseCase(clock)
    }

    @Test
    fun getEventStats_countsUpcomingPastAndThisWeek() {
        val events = listOf(
            event("1", now.plus(1, ChronoUnit.DAYS)),
            event("2", now.minus(1, ChronoUnit.DAYS)),
            event("3", now.plus(3, ChronoUnit.DAYS))
        )

        val stats = statsUseCase(events)

        assertEquals(3, stats.total)
        assertEquals(2, stats.upcoming)
        assertEquals(1, stats.past)
        assertEquals(2, stats.thisWeek)
        assertEquals(33, stats.completionPercent)
    }

    @Test
    fun getNextUpcomingEvent_returnsSoonestFuture() {
        val sooner = event("a", now.plus(2, ChronoUnit.HOURS))
        val later = event("b", now.plus(2, ChronoUnit.DAYS))
        val past = event("c", now.minus(1, ChronoUnit.DAYS))

        assertEquals(sooner, nextUseCase(listOf(later, past, sooner)))
    }

    @Test
    fun getNextUpcomingEvent_returnsNullWhenNone() {
        assertNull(nextUseCase(listOf(event("p", now.minus(1, ChronoUnit.DAYS)))))
    }

    @Test
    fun monthlyCounts_andBusiestMonth() {
        val events = listOf(
            event("1", Instant.parse("2026-01-10T10:00:00Z")),
            event("2", Instant.parse("2026-01-20T10:00:00Z")),
            event("3", Instant.parse("2026-02-01T10:00:00Z"))
        )

        val counts = monthlyUseCase(events)
        assertEquals(2, counts.values.maxOrNull())
        assertEquals(monthlyUseCase.busiestMonth(events), counts.maxByOrNull { it.value }?.key)
        assertEquals(1.5, monthlyUseCase.monthlyAverage(events), 0.001)
    }

    @Test
    fun nextSevenDays_aggregatesDailyLoad() {
        val events = listOf(
            event("1", now),
            event("2", now.plus(1, ChronoUnit.DAYS)),
            event("3", now.plus(1, ChronoUnit.DAYS)),
            event("4", now.plus(10, ChronoUnit.DAYS))
        )

        val days = weekUseCase(events)

        assertEquals(7, days.size)
        assertEquals(1, days[0].count)
        assertEquals(true, days[0].isToday)
        assertEquals(2, days[1].count)
        assertEquals(0, days[2].count)
    }

    private fun event(id: String, dateTime: Instant) = Event(
        id = id,
        title = "Event $id",
        dateTime = dateTime
    )
}
