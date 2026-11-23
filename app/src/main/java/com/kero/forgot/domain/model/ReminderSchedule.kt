package com.kero.forgot.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

sealed interface ReminderSchedule {
    val isPeriodic: Boolean

    data class OneShot(
        val date: LocalDate
    ) : ReminderSchedule {
        override val isPeriodic: Boolean = false
    }

    data class Daily(
        val intervalDays: Int = 1,
        val startDate: LocalDate = LocalDate.now()
    ) : ReminderSchedule {
        init {
            require(intervalDays >= 1) { "intervalDays must be >= 1" }
        }
        override val isPeriodic: Boolean = true
    }

    data class Weekly(
        val daysOfWeek: Set<DayOfWeek>,
        val startDate: LocalDate = LocalDate.now()
    ) : ReminderSchedule {
        override val isPeriodic: Boolean = true
    }

    data class Monthly(
        val dayOfMonth: Int,
        val startDate: LocalDate = LocalDate.now()
    ) : ReminderSchedule {
        init {
            require(dayOfMonth in 1..31) { "dayOfMonth must be in 1..31" }
        }
        override val isPeriodic: Boolean = true
    }
}
