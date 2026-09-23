package com.example.eventmanagement.data.mapper

import com.example.eventmanagement.data.dto.EventDto
import com.example.eventmanagement.domain.model.Event
import com.google.firebase.Timestamp
import java.time.Instant

fun EventDto.toDomain(id: String): Event? {
    val dateTime = dateTime?.toInstant() ?: return null
    return Event(
        id = id,
        title = title,
        description = description,
        dateTime = dateTime,
        location = location,
        userId = userId,
        createdAt = createdAt?.toInstant(),
        updatedAt = updatedAt?.toInstant()
    )
}

fun Event.toWriteMap(createdAt: Timestamp?, updatedAt: Timestamp): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "dateTime" to Timestamp(dateTime.epochSecond, dateTime.nano),
    "location" to location,
    "userId" to userId,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
