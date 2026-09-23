package com.example.eventmanagement.domain.model

import java.time.Instant

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Instant = Instant.EPOCH,
    val location: String = "",
    val userId: String = "",
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

enum class EventFilter {
    ALL,
    UPCOMING,
    PAST
}
