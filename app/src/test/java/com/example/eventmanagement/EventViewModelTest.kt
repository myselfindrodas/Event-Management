package com.example.eventmanagement

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.data.model.Event
import com.example.eventmanagement.data.repository.EventRepository
import com.example.eventmanagement.ui.events.EventViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: EventRepository
    private lateinit var viewModel: EventViewModel
    private val eventsFlow = MutableStateFlow<Resource<List<Event>>>(Resource.Success(emptyList()))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        whenever(repository.observeEvents()).thenReturn(eventsFlow)
        viewModel = EventViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun computeStats_countsTotalUpcomingAndPast() {
        val now = Date()
        val upcoming = Event(
            id = "1",
            title = "Future",
            dateTime = Timestamp(Date(now.time + 86_400_000L))
        )
        val past = Event(
            id = "2",
            title = "Past",
            dateTime = Timestamp(Date(now.time - 86_400_000L))
        )

        val stats = viewModel.computeStats(listOf(upcoming, past))

        assertEquals(2, stats.total)
        assertEquals(1, stats.upcoming)
        assertEquals(1, stats.past)
    }

    @Test
    fun saveEvent_rejectsBlankTitle() = runTest {
        viewModel.saveEvent(
            eventId = null,
            title = "   ",
            description = "desc",
            date = Date(System.currentTimeMillis() + 3_600_000L),
            location = "Hall",
            isEdit = false
        )

        val result = viewModel.actionResult.value
        assertTrue(result is Resource.Error)
        assertEquals("Title is required.", (result as Resource.Error).message)
    }

    @Test
    fun saveEvent_allowsPastDateForNewEvent() = runTest {
        whenever(repository.addEvent(any())).thenReturn(Resource.Success("past-id"))
        val past = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time

        viewModel.saveEvent(
            eventId = null,
            title = "Meetup",
            description = "desc",
            date = past,
            location = "Hall",
            isEdit = false
        )

        val result = viewModel.actionResult.value
        assertTrue(result is Resource.Success)
    }

    @Test
    fun saveEvent_callsRepositoryOnValidInput() = runTest {
        whenever(repository.addEvent(any())).thenReturn(Resource.Success("new-id"))

        viewModel.saveEvent(
            eventId = null,
            title = "Meetup",
            description = "desc",
            date = Date(System.currentTimeMillis() + 3_600_000L),
            location = "Hall",
            isEdit = false
        )

        val result = viewModel.actionResult.value
        assertTrue(result is Resource.Success)
    }
}
