package com.example.eventmanagement.domain.model

data class EventStats(
    val total: Int = 0,
    val upcoming: Int = 0,
    val past: Int = 0,
    val thisWeek: Int = 0,
    val completionPercent: Int = 0
)

data class DayLoad(
    val label: String,
    val dayOfMonth: String,
    val count: Int,
    val isToday: Boolean
)
