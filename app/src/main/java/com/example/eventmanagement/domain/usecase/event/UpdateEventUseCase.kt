package com.example.eventmanagement.domain.usecase.event

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.validation.EventValidator
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.repository.EventRepository
import javax.inject.Inject

class UpdateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val eventValidator: EventValidator
) {
    suspend operator fun invoke(event: Event): Resource<Unit> {
        val validation = eventValidator.validateTitle(event.title)
        if (validation != null) return Resource.Error(validation)
        return eventRepository.updateEvent(event.copy(title = event.title.trim()))
    }
}
