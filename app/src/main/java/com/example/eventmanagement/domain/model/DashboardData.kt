package com.example.eventmanagement.domain.model

data class DashboardData(
    val stats: EventStats = EventStats(),
    val nextEvent: Event? = null,
    val nextEventDays: Int = 0,
    val weekLoad: List<DayLoad> = emptyList(),
    val busiestMonth: String? = null,
    val monthlyAverage: Double = 0.0,
    val monthlyCounts: Map<String, Int> = emptyMap()
)
