package com.example.eventmanagement.domain.usecase.event

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.repository.EventRepository
import javax.inject.Inject

class DeleteEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Resource<Unit> =
        eventRepository.deleteEvent(eventId)
}
