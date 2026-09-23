package com.example.eventmanagement.domain.session

import kotlinx.coroutines.flow.Flow

interface UserSession {
    val userId: Flow<String?>
    fun currentUserId(): String?
}
