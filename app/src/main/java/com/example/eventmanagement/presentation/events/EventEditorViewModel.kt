package com.example.eventmanagement.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.usecase.event.CreateEventUseCase
import com.example.eventmanagement.domain.usecase.event.GetEventUseCase
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

data class EventEditorUiState(
    val isLoading: Boolean = false,
    val event: Event? = null,
    val error: AppError? = null
)

sealed interface EventEditorUiEffect {
    data class Error(val error: AppError) : EventEditorUiEffect
    data object Created : EventEditorUiEffect
    data object Updated : EventEditorUiEffect
}

@HiltViewModel
class EventEditorViewModel @Inject constructor(
    private val getEventUseCase: GetEventUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventEditorUiState())
    val uiState: StateFlow<EventEditorUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EventEditorUiEffect>()
    val effects: SharedFlow<EventEditorUiEffect> = _effects.asSharedFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getEventUseCase(eventId)) {
                Resource.Loading -> Unit
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, event = result.data, error = null)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                    _effects.emit(EventEditorUiEffect.Error(result.error))
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
            _uiState.update { it.copy(isLoading = true) }
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
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.emit(
                        if (isEdit) EventEditorUiEffect.Updated else EventEditorUiEffect.Created
                    )
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.emit(EventEditorUiEffect.Error(result.error))
                }
            }
        }
    }
}
