package com.example.eventmanagement.domain.usecase.event

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.error.ValidationField
import com.example.eventmanagement.core.time.FakeAppClock
import com.example.eventmanagement.core.validation.EventValidator
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.model.EventFilter
import com.example.eventmanagement.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class EventUseCasesTest {

    private val now = Instant.parse("2026-06-15T12:00:00Z")
    private val clock = FakeAppClock(now)

    @Test
    fun filterEvents_appliesQueryAndUpcomingFilter() {
        val useCase = FilterEventsUseCase(clock)
        val events = listOf(
            Event(id = "1", title = "Team meetup", dateTime = now.plus(1, ChronoUnit.DAYS)),
            Event(id = "2", title = "Past meetup", dateTime = now.minus(1, ChronoUnit.DAYS)),
            Event(id = "3", title = "Other", dateTime = now.plus(2, ChronoUnit.DAYS))
        )

        val filtered = useCase(events, "meetup", EventFilter.UPCOMING)

        assertEquals(1, filtered.size)
        assertEquals("1", filtered.first().id)
    }

    @Test
    fun createEvent_rejectsBlankTitle() = runTest {
        val repo = FakeEventRepository()
        val useCase = CreateEventUseCase(repo, EventValidator())

        val result = useCase(Event(title = "   ", dateTime = now))

        assertTrue(result is Resource.Error)
        assertEquals(
            AppError.Validation(ValidationField.TITLE_REQUIRED),
            (result as Resource.Error).error
        )
    }

    @Test
    fun createEvent_allowsPastDate() = runTest {
        val repo = FakeEventRepository()
        val useCase = CreateEventUseCase(repo, EventValidator())

        val result = useCase(
            Event(title = "Past", dateTime = now.minus(2, ChronoUnit.DAYS))
        )

        assertTrue(result is Resource.Success)
        assertEquals("generated-id", (result as Resource.Success).data)
    }

    private class FakeEventRepository : EventRepository {
        override fun observeEvents(): Flow<Resource<List<Event>>> = flowOf(Resource.Success(emptyList()))
        override suspend fun addEvent(event: Event): Resource<String> = Resource.Success("generated-id")
        override suspend fun updateEvent(event: Event): Resource<Unit> = Resource.Success(Unit)
        override suspend fun deleteEvent(eventId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun getEvent(eventId: String): Resource<Event> = Resource.Error(AppError.NotFound)
    }
}
