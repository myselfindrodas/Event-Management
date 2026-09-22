package com.example.eventmanagement.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Event(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp = Timestamp.now(),
    val location: String = "",
    val userId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "dateTime" to dateTime,
        "location" to location,
        "userId" to userId,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

enum class EventFilter {
    ALL,
    UPCOMING,
    PAST
}
