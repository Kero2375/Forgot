package com.kero.forgot.domain.model

import java.time.LocalTime

data class Reminder(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val categoryId: Long,
    val isEnabled: Boolean = true,
    val schedule: ReminderSchedule,
    val triggerTimes: List<LocalTime>,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
