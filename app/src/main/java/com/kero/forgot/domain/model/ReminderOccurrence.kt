package com.kero.forgot.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class CompletionKey(
    val reminderId: Long,
    val date: LocalDate,
    val time: LocalTime
)

data class ReminderOccurrence(
    val reminderId: Long,
    val title: String,
    val description: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryColorArgb: Long,
    val date: LocalDate,
    val time: LocalTime,
    val isCompleted: Boolean,
    val isPeriodic: Boolean
) {
    val completionKey: CompletionKey
        get() = CompletionKey(reminderId = reminderId, date = date, time = time)
}
