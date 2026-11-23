package com.kero.forgot.data.local

import com.kero.forgot.data.local.dao.CategoryDao
import com.kero.forgot.data.local.dao.ReminderDao
import com.kero.forgot.data.local.dao.TodoDao
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.ReminderSchedule
import com.kero.forgot.domain.model.TodoItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class SeedDataInitializer(
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val todoDao: TodoDao
) {
    suspend fun seedIfEmpty() {
        if (categoryDao.count() > 0) return

        val today = LocalDate.now()

        val healthId = categoryDao.upsert(
            ReminderCategory(
                name = "Health",
                colorArgb = 0xFF0F766E,
                isEnabled = true,
                sortOrder = 0
            ).toEntity()
        )
        val homeId = categoryDao.upsert(
            ReminderCategory(
                name = "Home",
                colorArgb = 0xFFB45309,
                isEnabled = true,
                sortOrder = 1
            ).toEntity()
        )
        val workId = categoryDao.upsert(
            ReminderCategory(
                name = "Work",
                colorArgb = 0xFF1D4ED8,
                isEnabled = true,
                sortOrder = 2
            ).toEntity()
        )
        val personalId = categoryDao.upsert(
            ReminderCategory(
                name = "Personal",
                colorArgb = 0xFF7E22CE,
                isEnabled = true,
                sortOrder = 3
            ).toEntity()
        )

        reminderDao.upsert(
            Reminder(
                title = "Brush teeth",
                description = "Morning, after lunch, and before bed",
                categoryId = healthId,
                isEnabled = true,
                schedule = ReminderSchedule.Daily(intervalDays = 1, startDate = today),
                triggerTimes = listOf(
                    LocalTime.of(7, 30),
                    LocalTime.of(13, 30),
                    LocalTime.of(22, 0)
                )
            ).toEntity()
        )

        reminderDao.upsert(
            Reminder(
                title = "Take vitamins",
                description = "With breakfast",
                categoryId = healthId,
                isEnabled = true,
                schedule = ReminderSchedule.Daily(intervalDays = 1, startDate = today),
                triggerTimes = listOf(LocalTime.of(8, 15))
            ).toEntity()
        )

        reminderDao.upsert(
            Reminder(
                title = "Stretch & posture break",
                description = "5 minutes away from screen",
                categoryId = workId,
                isEnabled = true,
                schedule = ReminderSchedule.Weekly(
                    daysOfWeek = setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    ),
                    startDate = today
                ),
                triggerTimes = listOf(LocalTime.of(11, 0), LocalTime.of(16, 0))
            ).toEntity()
        )

        reminderDao.upsert(
            Reminder(
                title = "Water plants",
                description = "Balcony & living room",
                categoryId = homeId,
                isEnabled = true,
                schedule = ReminderSchedule.Daily(intervalDays = 3, startDate = today),
                triggerTimes = listOf(LocalTime.of(18, 30))
            ).toEntity()
        )

        reminderDao.upsert(
            Reminder(
                title = "Read 20 pages",
                description = "Wind down without phone",
                categoryId = personalId,
                isEnabled = false,
                schedule = ReminderSchedule.Daily(intervalDays = 1, startDate = today),
                triggerTimes = listOf(LocalTime.of(21, 15))
            ).toEntity()
        )

        todoDao.upsert(
            TodoItem(
                title = "Plan tomorrow's priorities",
                notes = "Swipe right to add tasks for tomorrow",
                targetDate = today,
                sortOrder = 0
            ).toEntity()
        )
        todoDao.upsert(
            TodoItem(
                title = "Review reminder categories",
                notes = "Toggle categories on or off anytime",
                targetDate = today,
                sortOrder = 1
            ).toEntity()
        )
    }
}
