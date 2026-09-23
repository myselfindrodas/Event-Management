package com.example.eventmanagement.domain.usecase.event

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.repository.EventRepository
import javax.inject.Inject

class GetEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Resource<Event> =
        eventRepository.getEvent(eventId)
}
