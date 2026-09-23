package com.example.eventmanagement.core.time

import java.time.Instant

class FakeAppClock(private var instant: Instant) : AppClock {
    override fun now(): Instant = instant
    fun set(instant: Instant) {
        this.instant = instant
    }
}
