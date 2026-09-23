package com.example.eventmanagement.domain.usecase.event

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.validation.EventValidator
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.repository.EventRepository
import javax.inject.Inject

class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val eventValidator: EventValidator
) {
    suspend operator fun invoke(event: Event): Resource<String> {
        val validation = eventValidator.validateTitle(event.title)
        if (validation != null) return Resource.Error(validation)
        return eventRepository.addEvent(event.copy(title = event.title.trim()))
    }
}
