package com.example.eventmanagement.domain.repository

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<Resource<List<Event>>>
    suspend fun addEvent(event: Event): Resource<String>
    suspend fun updateEvent(event: Event): Resource<Unit>
    suspend fun deleteEvent(eventId: String): Resource<Unit>
    suspend fun getEvent(eventId: String): Resource<Event>
}
