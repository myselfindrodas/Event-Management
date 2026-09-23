package com.example.eventmanagement.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.model.EventFilter
import com.example.eventmanagement.domain.usecase.event.CreateEventUseCase
import com.example.eventmanagement.domain.usecase.event.DeleteEventUseCase
import com.example.eventmanagement.domain.usecase.event.FilterEventsUseCase
import com.example.eventmanagement.domain.usecase.event.GetEventUseCase
import com.example.eventmanagement.domain.usecase.event.ObserveEventsUseCase
import com.example.eventmanagement.domain.usecase.event.UpdateEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class EventListUiState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val searchQuery: String = "",
    val filter: EventFilter = EventFilter.ALL,
    val error: AppError? = null
)

data class EventEditorUiState(
    val isLoading: Boolean = false,
    val event: Event? = null,
    val error: AppError? = null
)

sealed interface EventUiEffect {
    data class Error(val error: AppError) : EventUiEffect
    data object Deleted : EventUiEffect
    data object Created : EventUiEffect
    data object Updated : EventUiEffect
}

@HiltViewModel
class EventViewModel @Inject constructor(
    observeEventsUseCase: ObserveEventsUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val getEventUseCase: GetEventUseCase,
    private val filterEventsUseCase: FilterEventsUseCase
) : ViewModel() {

    private val allEvents = MutableStateFlow<List<Event>>(emptyList())

    private val _uiState = MutableStateFlow(EventListUiState(isLoading = true))
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    private val _editorState = MutableStateFlow(EventEditorUiState())
    val editorState: StateFlow<EventEditorUiState> = _editorState.asStateFlow()

    private val _effects = MutableSharedFlow<EventUiEffect>()
    val effects: SharedFlow<EventUiEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeEventsUseCase().collect { resource ->
                when (resource) {
                    Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        allEvents.value = resource.data
                        publishFiltered()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = resource.error) }
                        _effects.emit(EventUiEffect.Error(resource.error))
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        publishFiltered()
    }

    fun setFilter(filter: EventFilter) {
        _uiState.update { it.copy(filter = filter) }
        publishFiltered()
    }

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _editorState.update { it.copy(isLoading = true, error = null) }
            when (val result = getEventUseCase(eventId)) {
                Resource.Loading -> Unit
                is Resource.Success -> _editorState.update {
                    it.copy(isLoading = false, event = result.data, error = null)
                }
                is Resource.Error -> {
                    _editorState.update { it.copy(isLoading = false, error = result.error) }
                    _effects.emit(EventUiEffect.Error(result.error))
                }
            }
        }
    }

    fun saveEvent(
        eventId: String?,
        title: String,
        description: String,
        dateTime: Instant,
        location: String,
        isEdit: Boolean
    ) {
        val event = Event(
            id = eventId.orEmpty(),
            title = title.trim(),
            description = description.trim(),
            dateTime = dateTime,
            location = location.trim()
        )
        viewModelScope.launch {
            _editorState.update { it.copy(isLoading = true) }
            val result = if (isEdit) {
                updateEventUseCase(event)
            } else {
                when (val created = createEventUseCase(event)) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(created.error)
                    Resource.Loading -> Resource.Loading
                }
            }
            when (result) {
                Resource.Loading -> Unit
                is Resource.Success -> {
                    _editorState.update { it.copy(isLoading = false) }
                    _effects.emit(if (isEdit) EventUiEffect.Updated else EventUiEffect.Created)
                }
                is Resource.Error -> {
                    _editorState.update { it.copy(isLoading = false) }
                    _effects.emit(EventUiEffect.Error(result.error))
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            when (val result = deleteEventUseCase(eventId)) {
                Resource.Loading -> Unit
                is Resource.Success -> _effects.emit(EventUiEffect.Deleted)
                is Resource.Error -> _effects.emit(EventUiEffect.Error(result.error))
            }
        }
    }

    private fun publishFiltered() {
        val state = _uiState.value
        val filtered = filterEventsUseCase(allEvents.value, state.searchQuery, state.filter)
        _uiState.update {
            it.copy(isLoading = false, events = filtered, error = null)
        }
    }
}
