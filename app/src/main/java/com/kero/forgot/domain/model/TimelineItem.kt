package com.kero.forgot.domain.model

import java.time.LocalTime

sealed interface TimelineItem {
    data class TimeSlot(
        val time: LocalTime,
        val occurrences: List<ReminderOccurrence>
    ) : TimelineItem

    data class TimeGap(
        val startTime: LocalTime,
        val endTime: LocalTime,
        val durationMinutes: Long
    ) : TimelineItem
}
