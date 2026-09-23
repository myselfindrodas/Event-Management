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
class EventListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-06-15T12:00:00Z")
    private lateinit var repository: FakeEventRepository
    private lateinit var viewModel: EventListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeEventRepository()
        viewModel = EventListViewModel(
            observeEventsUseCase = ObserveEventsUseCase(repository),
            filterEventsUseCase = FilterEventsUseCase(FakeAppClock(now)),
            deleteEventUseCase = DeleteEventUseCase(repository)
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
    fun setSearchQuery_filtersByTitle() = runTest {
        val match = Event(id = "1", title = "Meetup", dateTime = now.plus(1, ChronoUnit.DAYS))
        val other = Event(id = "2", title = "Party", dateTime = now.plus(2, ChronoUnit.DAYS))
        repository.emit(Resource.Success(listOf(match, other)))

        viewModel.setSearchQuery("meet")

        assertEquals(listOf(match), viewModel.uiState.value.events)
    }

    @Test
    fun observeError_emitsEffect() = runTest {
        viewModel.effects.test {
            repository.emit(Resource.Error(AppError.Network))
            val effect = awaitItem()
            assertTrue(effect is EventListUiEffect.Error)
            assertEquals(AppError.Network, (effect as EventListUiEffect.Error).error)
        }
    }

    @Test
    fun deleteEvent_successEmitsDeleted() = runTest {
        viewModel.effects.test {
            viewModel.deleteEvent("1")
            assertEquals(EventListUiEffect.Deleted, awaitItem())
        }
    }

    @Test
    fun deleteEvent_failureEmitsError() = runTest {
        repository.deleteResult = Resource.Error(AppError.NotFound)
        viewModel.effects.test {
            viewModel.deleteEvent("missing")
            val effect = awaitItem()
            assertTrue(effect is EventListUiEffect.Error)
        }
    }

    private class FakeEventRepository : EventRepository {
        private val events = MutableStateFlow<Resource<List<Event>>>(Resource.Success(emptyList()))
        var deleteResult: Resource<Unit> = Resource.Success(Unit)

        fun emit(value: Resource<List<Event>>) {
            events.value = value
        }

        override fun observeEvents(): Flow<Resource<List<Event>>> = events
        override suspend fun addEvent(event: Event): Resource<String> = Resource.Success("new-id")
        override suspend fun updateEvent(event: Event): Resource<Unit> = Resource.Success(Unit)
        override suspend fun deleteEvent(eventId: String): Resource<Unit> = deleteResult
        override suspend fun getEvent(eventId: String): Resource<Event> =
            Resource.Error(AppError.NotFound)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EventEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-06-15T12:00:00Z")
    private lateinit var repository: FakeEventRepository
    private lateinit var viewModel: EventEditorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeEventRepository()
        val validator = EventValidator()
        viewModel = EventEditorViewModel(
            getEventUseCase = GetEventUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository, validator),
            updateEventUseCase = UpdateEventUseCase(repository, validator)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadEvent_success() = runTest {
        val event = Event(id = "1", title = "Meetup", dateTime = now)
        repository.getResult = Resource.Success(event)

        viewModel.loadEvent("1")

        assertEquals(event, viewModel.uiState.value.event)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun loadEvent_notFoundEmitsError() = runTest {
        repository.getResult = Resource.Error(AppError.NotFound)
        viewModel.effects.test {
            viewModel.loadEvent("missing")
            val effect = awaitItem()
            assertTrue(effect is EventEditorUiEffect.Error)
            assertEquals(AppError.NotFound, (effect as EventEditorUiEffect.Error).error)
        }
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
            assertTrue(effect is EventEditorUiEffect.Error)
            assertEquals(
                AppError.Validation(ValidationField.TITLE_REQUIRED),
                (effect as EventEditorUiEffect.Error).error
            )
        }
    }

    @Test
    fun saveEvent_createSuccess() = runTest {
        viewModel.effects.test {
            viewModel.saveEvent(
                eventId = null,
                title = "Meetup",
                description = "desc",
                dateTime = now.minus(1, ChronoUnit.DAYS),
                location = "Hall",
                isEdit = false
            )
            assertEquals(EventEditorUiEffect.Created, awaitItem())
        }
    }

    @Test
    fun saveEvent_updateSuccess() = runTest {
        viewModel.effects.test {
            viewModel.saveEvent(
                eventId = "1",
                title = "Updated",
                description = "desc",
                dateTime = now,
                location = "Hall",
                isEdit = true
            )
            assertEquals(EventEditorUiEffect.Updated, awaitItem())
        }
    }

    @Test
    fun saveEvent_createError() = runTest {
        repository.addResult = Resource.Error(AppError.Network)
        viewModel.effects.test {
            viewModel.saveEvent(
                eventId = null,
                title = "Meetup",
                description = "",
                dateTime = now,
                location = "",
                isEdit = false
            )
            val effect = awaitItem()
            assertTrue(effect is EventEditorUiEffect.Error)
            assertEquals(AppError.Network, (effect as EventEditorUiEffect.Error).error)
        }
    }

    private class FakeEventRepository : EventRepository {
        var getResult: Resource<Event> = Resource.Error(AppError.NotFound)
        var addResult: Resource<String> = Resource.Success("new-id")
        var updateResult: Resource<Unit> = Resource.Success(Unit)

        override fun observeEvents(): Flow<Resource<List<Event>>> =
            MutableStateFlow(Resource.Success(emptyList()))

        override suspend fun addEvent(event: Event): Resource<String> = addResult
        override suspend fun updateEvent(event: Event): Resource<Unit> = updateResult
        override suspend fun deleteEvent(eventId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun getEvent(eventId: String): Resource<Event> = getResult
    }
}
