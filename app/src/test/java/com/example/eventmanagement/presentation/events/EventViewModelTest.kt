package com.example.eventmanagement.presentation.events

import app.cash.turbine.test
import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.error.ValidationField
import com.example.eventmanagement.core.time.FakeAppClock
import com.example.eventmanagement.core.validation.EventValidator
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.model.EventFilter
import com.example.eventmanagement.domain.repository.EventRepository
import com.example.eventmanagement.domain.usecase.event.CreateEventUseCase
import com.example.eventmanagement.domain.usecase.event.DeleteEventUseCase
import com.example.eventmanagement.domain.usecase.event.FilterEventsUseCase
import com.example.eventmanagement.domain.usecase.event.GetEventUseCase
import com.example.eventmanagement.domain.usecase.event.ObserveEventsUseCase
import com.example.eventmanagement.domain.usecase.event.UpdateEventUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-06-15T12:00:00Z")
    private lateinit var repository: FakeEventRepository
    private lateinit var viewModel: EventViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeEventRepository()
        val clock = FakeAppClock(now)
        viewModel = EventViewModel(
            observeEventsUseCase = ObserveEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository, EventValidator()),
            updateEventUseCase = UpdateEventUseCase(repository, EventValidator()),
            deleteEventUseCase = DeleteEventUseCase(repository),
            getEventUseCase = GetEventUseCase(repository),
            filterEventsUseCase = FilterEventsUseCase(clock)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeEvents_updatesUiState() = runTest {
        val upcoming = Event(id = "1", title = "Future", dateTime = now.plus(1, ChronoUnit.DAYS))
        repository.emit(Resource.Success(listOf(upcoming)))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isLoading)
            assertEquals(1, state.events.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setFilter_pastHidesUpcoming() = runTest {
        val upcoming = Event(id = "1", title = "Future", dateTime = now.plus(1, ChronoUnit.DAYS))
        val past = Event(id = "2", title = "Past", dateTime = now.minus(1, ChronoUnit.DAYS))
        repository.emit(Resource.Success(listOf(upcoming, past)))

        viewModel.setFilter(EventFilter.PAST)

        assertEquals(listOf(past), viewModel.uiState.value.events)
    }

    @Test
    fun saveEvent_rejectsBlankTitle() = runTest {
        viewModel.effects.test {
            viewModel.saveEvent(
                eventId = null,
                title = "   ",
                description = "desc",
                dateTime = now.plus(1, ChronoUnit.HOURS),
                location = "Hall",
                isEdit = false
            )
            val effect = awaitItem()
            assertTrue(effect is EventUiEffect.Error)
            assertEquals(
                AppError.Validation(ValidationField.TITLE_REQUIRED),
                (effect as EventUiEffect.Error).error
            )
        }
    }

    @Test
    fun saveEvent_allowsPastDate() = runTest {
        viewModel.effects.test {
            viewModel.saveEvent(
                eventId = null,
                title = "Meetup",
                description = "desc",
                dateTime = now.minus(1, ChronoUnit.DAYS),
                location = "Hall",
                isEdit = false
            )
            assertEquals(EventUiEffect.Created, awaitItem())
        }
    }

    private class FakeEventRepository : EventRepository {
        private val events = MutableStateFlow<Resource<List<Event>>>(Resource.Success(emptyList()))

        fun emit(value: Resource<List<Event>>) {
            events.value = value
        }

        override fun observeEvents(): Flow<Resource<List<Event>>> = events
        override suspend fun addEvent(event: Event): Resource<String> = Resource.Success("new-id")
        override suspend fun updateEvent(event: Event): Resource<Unit> = Resource.Success(Unit)
        override suspend fun deleteEvent(eventId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun getEvent(eventId: String): Resource<Event> =
            Resource.Error(AppError.NotFound)
    }
}
