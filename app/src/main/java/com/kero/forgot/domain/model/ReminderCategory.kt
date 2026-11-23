package com.kero.forgot.domain.model

import java.time.LocalDate

data class ReminderCategory(
    val id: Long = 0L,
    val name: String,
    val colorArgb: Long,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val activeFrom: LocalDate? = null,
    val activeUntil: LocalDate? = null
) {
    val isScheduledRange: Boolean
        get() = activeFrom != null || activeUntil != null

    fun isActiveOn(date: LocalDate): Boolean {
        if (!isEnabled) return false
        if (activeFrom != null && date.isBefore(activeFrom)) return false
        if (activeUntil != null && date.isAfter(activeUntil)) return false
        return true
    }
}
