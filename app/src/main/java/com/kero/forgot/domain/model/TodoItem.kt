package com.kero.forgot.domain.model

import java.time.LocalDate

data class TodoItem(
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val targetDate: LocalDate,
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
