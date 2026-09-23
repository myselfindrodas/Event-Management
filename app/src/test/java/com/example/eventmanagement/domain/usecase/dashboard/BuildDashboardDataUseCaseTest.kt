package com.example.eventmanagement.domain.usecase.dashboard

import com.example.eventmanagement.core.time.FakeAppClock
import com.example.eventmanagement.domain.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class BuildDashboardDataUseCaseTest {

    private lateinit var useCase: BuildDashboardDataUseCase
    private val now = Instant.parse("2026-06-15T12:00:00Z")

    @Before
    fun setup() {
        val clock = FakeAppClock(now)
        useCase = BuildDashboardDataUseCase(
            getEventStatsUseCase = GetEventStatsUseCase(clock),
            getNextUpcomingEventUseCase = GetNextUpcomingEventUseCase(clock),
            getMonthlyEventCountsUseCase = GetMonthlyEventCountsUseCase(),
            getNextSevenDaysUseCase = GetNextSevenDaysUseCase(clock),
            clock = clock
        )
    }

    @Test
    fun emptyList_returnsZeros() {
        val data = useCase(emptyList())
        assertEquals(0, data.stats.total)
        assertNull(data.nextEvent)
        assertEquals(0.0, data.monthlyAverage, 0.0)
        assertEquals(7, data.weekLoad.size)
    }

    @Test
    fun mixedEvents_buildsAnalytics() {
        val events = listOf(
            Event(id = "1", title = "Soon", dateTime = now.plus(1, ChronoUnit.DAYS)),
            Event(id = "2", title = "Past", dateTime = now.minus(2, ChronoUnit.DAYS)),
            Event(id = "3", title = "Week", dateTime = now.plus(3, ChronoUnit.DAYS))
        )

        val data = useCase(events)

        assertEquals(3, data.stats.total)
        assertEquals(2, data.stats.upcoming)
        assertEquals(1, data.stats.past)
        assertEquals("1", data.nextEvent?.id)
        assertEquals(1, data.nextEventDays)
        assertEquals(1, data.weekLoad[1].count)
        assertEquals(1, data.weekLoad[3].count)
    }
}
