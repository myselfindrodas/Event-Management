package com.example.eventmanagement.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.domain.model.DayLoad
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.model.EventStats
import com.example.eventmanagement.domain.usecase.auth.GetCurrentUserUseCase
import com.example.eventmanagement.domain.usecase.dashboard.GetEventStatsUseCase
import com.example.eventmanagement.domain.usecase.dashboard.GetMonthlyEventCountsUseCase
import com.example.eventmanagement.domain.usecase.dashboard.GetNextSevenDaysUseCase
import com.example.eventmanagement.domain.usecase.dashboard.GetNextUpcomingEventUseCase
import com.example.eventmanagement.domain.usecase.event.ObserveEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Greeting {
    MORNING, AFTERNOON, EVENING
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val greeting: Greeting = Greeting.MORNING,
    val todayLabel: String = "",
    val stats: EventStats = EventStats(),
    val nextEvent: Event? = null,
    val nextEventDays: Int = 0,
    val weekLoad: List<DayLoad> = emptyList(),
    val busiestMonth: String? = null,
    val monthlyAverage: Double = 0.0,
    val monthlyCounts: Map<String, Int> = emptyMap(),
    val error: AppError? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeEventsUseCase: ObserveEventsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getEventStatsUseCase: GetEventStatsUseCase,
    private val getNextUpcomingEventUseCase: GetNextUpcomingEventUseCase,
    private val getMonthlyEventCountsUseCase: GetMonthlyEventCountsUseCase,
    private val getNextSevenDaysUseCase: GetNextSevenDaysUseCase,
    private val clock: AppClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val now = clock.now()
        val email = getCurrentUserUseCase()?.email.orEmpty()
        val displayName = email.substringBefore('@').ifBlank { "" }
        _uiState.update {
            it.copy(
                displayName = displayName,
                greeting = greetingForHour(DateFormatter.hourOfDay(now)),
                todayLabel = DateFormatter.todayLong(now)
            )
        }
        viewModelScope.launch {
            observeEventsUseCase().collect { resource ->
                when (resource) {
                    Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> bindEvents(resource.data)
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = resource.error)
                    }
                }
            }
        }
    }

    private fun bindEvents(events: List<Event>) {
        val next = getNextUpcomingEventUseCase(events)
        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                stats = getEventStatsUseCase(events),
                nextEvent = next,
                nextEventDays = next?.let { event -> DateFormatter.daysUntil(event.dateTime, clock.now()) } ?: 0,
                weekLoad = getNextSevenDaysUseCase(events),
                busiestMonth = getMonthlyEventCountsUseCase.busiestMonth(events),
                monthlyAverage = getMonthlyEventCountsUseCase.monthlyAverage(events),
                monthlyCounts = getMonthlyEventCountsUseCase(events)
            )
        }
    }

    private fun greetingForHour(hour: Int): Greeting = when (hour) {
        in 5..11 -> Greeting.MORNING
        in 12..16 -> Greeting.AFTERNOON
        else -> Greeting.EVENING
    }
}
