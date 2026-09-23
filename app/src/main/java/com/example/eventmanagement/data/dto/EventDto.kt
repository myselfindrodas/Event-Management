package com.example.eventmanagement.data.dto

import com.google.firebase.Timestamp

data class EventDto(
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp? = null,
    val location: String = "",
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
