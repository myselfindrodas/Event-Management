package com.example.eventmanagement.core.time

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface AppClock {
    fun now(): Instant
}

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun now(): Instant = Instant.now()
}
