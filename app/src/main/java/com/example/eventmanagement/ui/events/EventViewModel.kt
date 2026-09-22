package com.example.eventmanagement.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.data.model.Event
import com.example.eventmanagement.data.model.EventFilter
import com.example.eventmanagement.data.repository.EventRepository
import com.example.eventmanagement.util.DateUtils
import com.example.eventmanagement.util.Validators
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class EventViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _searchQuery = MutableLiveData("")
    private val _filter = MutableLiveData(EventFilter.ALL)

    val events: LiveData<Resource<List<Event>>> = eventRepository.observeEvents().asLiveData()

    private val _actionResult = MutableLiveData<Resource<Unit>>()
    val actionResult: LiveData<Resource<Unit>> = _actionResult

    private val _selectedEvent = MutableLiveData<Resource<Event>>()
    val selectedEvent: LiveData<Resource<Event>> = _selectedEvent

    val filteredEvents: LiveData<List<Event>> = MediatorLiveData<List<Event>>().apply {
        fun update() {
            val resource = events.value
            val list = (resource as? Resource.Success)?.data.orEmpty()
            val query = _searchQuery.value.orEmpty().trim().lowercase()
            val filter = _filter.value ?: EventFilter.ALL
            val now = Date()

            value = list.filter { event ->
                val matchesQuery = query.isEmpty() ||
                    event.title.lowercase().contains(query) ||
                    event.description.lowercase().contains(query) ||
                    event.location.lowercase().contains(query)

                val matchesFilter = when (filter) {
                    EventFilter.ALL -> true
                    EventFilter.UPCOMING -> !event.dateTime.toDate().before(now)
                    EventFilter.PAST -> event.dateTime.toDate().before(now)
                }
                matchesQuery && matchesFilter
            }
        }

        addSource(events) { update() }
        addSource(_searchQuery) { update() }
        addSource(_filter) { update() }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: EventFilter) {
        _filter.value = filter
    }

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _selectedEvent.value = Resource.Loading
            _selectedEvent.value = eventRepository.getEvent(eventId)
        }
    }

    fun saveEvent(
        eventId: String?,
        title: String,
        description: String,
        date: Date,
        location: String,
        isEdit: Boolean
    ) {
        val titleError = Validators.validateEventTitle(title)
        if (titleError != null) {
            _actionResult.value = Resource.Error(titleError)
            return
        }

        val event = Event(
            id = eventId.orEmpty(),
            title = title.trim(),
            description = description.trim(),
            dateTime = Timestamp(date),
            location = location.trim()
        )

        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            _actionResult.value = if (isEdit) {
                eventRepository.updateEvent(event)
            } else {
                when (val result = eventRepository.addEvent(event)) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(result.message)
                    Resource.Loading -> Resource.Loading
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            _actionResult.value = eventRepository.deleteEvent(eventId)
        }
    }

    fun computeStats(events: List<Event>): EventStats {
        val now = Date()
        val upcoming = events.count { !it.dateTime.toDate().before(now) }
        val past = events.count { it.dateTime.toDate().before(now) }
        val thisWeek = events.count {
            val days = DateUtils.daysUntil(it.dateTime.toDate())
            days in 0..6
        }
        val completion = if (events.isEmpty()) 0 else (past * 100) / events.size
        return EventStats(
            total = events.size,
            upcoming = upcoming,
            past = past,
            thisWeek = thisWeek,
            completionPercent = completion
        )
    }

    fun nextSevenDays(events: List<Event>): List<DayLoad> {
        val counts = IntArray(7)
        events.forEach { event ->
            val offset = DateUtils.daysUntil(event.dateTime.toDate())
            if (offset in 0..6) counts[offset]++
        }
        val calendar = Calendar.getInstance()
        return (0..6).map { offset ->
            val day = (calendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }.time
            DayLoad(
                label = DateUtils.weekdayShort(day),
                dayOfMonth = DateUtils.dayOfMonth(day),
                count = counts[offset],
                isToday = offset == 0
            )
        }
    }

    fun busiestMonth(events: List<Event>): String? {
        return eventsPerMonth(events).maxByOrNull { it.value }?.key
    }

    fun monthlyAverage(events: List<Event>): Double {
        val perMonth = eventsPerMonth(events)
        if (perMonth.isEmpty()) return 0.0
        return events.size.toDouble() / perMonth.size
    }

    fun nextUpcomingEvent(events: List<Event>): Event? {
        val now = Date()
        return events
            .asSequence()
            .filter { !it.dateTime.toDate().before(now) }
            .minByOrNull { it.dateTime.toDate().time }
    }

    fun eventsPerMonth(events: List<Event>): Map<String, Int> {
        return events
            .groupBy { DateUtils.monthKey(it.dateTime) }
            .mapValues { it.value.size }
            .toSortedMap(
                compareBy { key ->
                    events.firstOrNull { DateUtils.monthKey(it.dateTime) == key }
                        ?.dateTime?.toDate()?.time ?: 0L
                }
            )
    }

    class Factory(
        private val eventRepository: EventRepository = EventRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EventViewModel(eventRepository) as T
        }
    }
}

data class EventStats(
    val total: Int,
    val upcoming: Int,
    val past: Int,
    val thisWeek: Int = 0,
    val completionPercent: Int = 0
)

data class DayLoad(
    val label: String,
    val dayOfMonth: String,
    val count: Int,
    val isToday: Boolean
)
