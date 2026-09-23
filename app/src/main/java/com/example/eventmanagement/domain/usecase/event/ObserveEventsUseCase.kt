package com.example.eventmanagement.domain.usecase.event

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<Resource<List<Event>>> = eventRepository.observeEvents()
}
