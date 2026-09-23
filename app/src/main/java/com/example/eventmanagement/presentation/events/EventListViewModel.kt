package com.example.eventmanagement.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.model.EventFilter
import com.example.eventmanagement.domain.usecase.event.DeleteEventUseCase
import com.example.eventmanagement.domain.usecase.event.FilterEventsUseCase
import com.example.eventmanagement.domain.usecase.event.ObserveEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventListUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val searchQuery: String = "",
    val filter: EventFilter = EventFilter.ALL,
    val error: AppError? = null
)

sealed interface EventListUiEffect {
    data class Error(val error: AppError) : EventListUiEffect
    data object Deleted : EventListUiEffect
}

@HiltViewModel
class EventListViewModel @Inject constructor(
    observeEventsUseCase: ObserveEventsUseCase,
    private val filterEventsUseCase: FilterEventsUseCase,
    private val deleteEventUseCase: DeleteEventUseCase
) : ViewModel() {

    private val allEvents = MutableStateFlow<List<Event>>(emptyList())

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EventListUiEffect>()
    val effects: SharedFlow<EventListUiEffect> = _effects.asSharedFlow()

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
                        _effects.emit(EventListUiEffect.Error(resource.error))
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

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            when (val result = deleteEventUseCase(eventId)) {
                Resource.Loading -> Unit
                is Resource.Success -> _effects.emit(EventListUiEffect.Deleted)
                is Resource.Error -> _effects.emit(EventListUiEffect.Error(result.error))
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
