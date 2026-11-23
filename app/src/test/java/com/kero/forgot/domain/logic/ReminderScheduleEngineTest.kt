package com.kero.forgot.domain.logic

import com.kero.forgot.data.local.decodeDaysMask
import com.kero.forgot.data.local.decodeTriggerTimes
import com.kero.forgot.data.local.encodeDaysMask
import com.kero.forgot.data.local.encodeTriggerTimes
import com.kero.forgot.domain.model.CompletionKey
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.ReminderSchedule
import com.kero.forgot.domain.model.TimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderScheduleEngineTest {

    private val healthCategory = ReminderCategory(
        id = 1L,
        name = "Health",
        colorArgb = 0xFF0F766E,
        isEnabled = true
    )

    private val pausedCategory = ReminderCategory(
        id = 2L,
        name = "Paused",
        colorArgb = 0xFF475569,
        isEnabled = false
    )

    @Test
    fun `multi-time periodic reminder produces sorted occurrences and skips gaps on timeline`() {
        val today = LocalDate.of(2026, 11, 20)
        val brushTeeth = Reminder(
            id = 10L,
            title = "Brush teeth",
            categoryId = healthCategory.id,
            isEnabled = true,
            schedule = ReminderSchedule.Daily(intervalDays = 1, startDate = today),
            triggerTimes = listOf(
                LocalTime.of(22, 0),
                LocalTime.of(7, 30),
                LocalTime.of(13, 30)
            )
        )
        val completions = setOf(
            CompletionKey(reminderId = 10L, date = today, time = LocalTime.of(7, 30))
        )

        val occurrences = ReminderScheduleEngine.occurrencesForDate(
            reminders = listOf(brushTeeth),
            categoriesById = mapOf(healthCategory.id to healthCategory),
            completions = completions,
            date = today
        )

        assertEquals(3, occurrences.size)
        assertEquals(LocalTime.of(7, 30), occurrences[0].time)
        assertTrue(occurrences[0].isCompleted)
        assertEquals(LocalTime.of(13, 30), occurrences[1].time)
        assertFalse(occurrences[1].isCompleted)
        assertEquals(LocalTime.of(22, 0), occurrences[2].time)

        val timeline = ReminderScheduleEngine.buildTimeline(occurrences, gapThresholdMinutes = 60L)
        // Expected: Slot(07:30), Gap(360m), Slot(13:30), Gap(510m), Slot(22:00)
        assertEquals(5, timeline.size)
        assertTrue(timeline[0] is TimelineItem.TimeSlot)
        assertTrue(timeline[1] is TimelineItem.TimeGap)
        assertEquals(360L, (timeline[1] as TimelineItem.TimeGap).durationMinutes)
        assertTrue(timeline[2] is TimelineItem.TimeSlot)
        assertTrue(timeline[3] is TimelineItem.TimeGap)
        assertEquals(510L, (timeline[3] as TimelineItem.TimeGap).durationMinutes)
        assertTrue(timeline[4] is TimelineItem.TimeSlot)
    }

    @Test
    fun `disabled category hides all reminders in that category`() {
        val today = LocalDate.of(2026, 11, 20)
        val reminder = Reminder(
            id = 20L,
            title = "Paused reminder",
            categoryId = pausedCategory.id,
            isEnabled = true,
            schedule = ReminderSchedule.Daily(intervalDays = 1, startDate = today),
            triggerTimes = listOf(LocalTime.of(9, 0))
        )

        val occurrences = ReminderScheduleEngine.occurrencesForDate(
            reminders = listOf(reminder),
            categoriesById = mapOf(pausedCategory.id to pausedCategory),
            completions = emptySet(),
            date = today
        )

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `one-shot, daily interval, weekly, and monthly schedules evaluate accurately`() {
        val start = LocalDate.of(2026, 11, 20) // Friday

        val oneShot = Reminder(
            id = 1L,
            title = "Dentist",
            categoryId = 1L,
            schedule = ReminderSchedule.OneShot(date = start.plusDays(2)),
            triggerTimes = listOf(LocalTime.of(10, 0))
        )
        assertFalse(ReminderScheduleEngine.occursOn(oneShot, start))
        assertTrue(ReminderScheduleEngine.occursOn(oneShot, start.plusDays(2)))
        assertFalse(ReminderScheduleEngine.occursOn(oneShot, start.plusDays(3)))

        val every3Days = Reminder(
            id = 2L,
            title = "Water plants",
            categoryId = 1L,
            schedule = ReminderSchedule.Daily(intervalDays = 3, startDate = start),
            triggerTimes = listOf(LocalTime.of(18, 0))
        )
        assertTrue(ReminderScheduleEngine.occursOn(every3Days, start))
        assertFalse(ReminderScheduleEngine.occursOn(every3Days, start.plusDays(1)))
        assertTrue(ReminderScheduleEngine.occursOn(every3Days, start.plusDays(3)))

        val weekly = Reminder(
            id = 3L,
            title = "Gym",
            categoryId = 1L,
            schedule = ReminderSchedule.Weekly(
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                startDate = start
            ),
            triggerTimes = listOf(LocalTime.of(19, 0))
        )
        assertTrue(ReminderScheduleEngine.occursOn(weekly, start)) // Friday
        assertFalse(ReminderScheduleEngine.occursOn(weekly, start.plusDays(1))) // Saturday
        assertTrue(ReminderScheduleEngine.occursOn(weekly, start.plusDays(3))) // Monday

        val monthly31 = Reminder(
            id = 4L,
            title = "Pay rent",
            categoryId = 1L,
            schedule = ReminderSchedule.Monthly(dayOfMonth = 31, startDate = LocalDate.of(2026, 1, 1)),
            triggerTimes = listOf(LocalTime.of(9, 0))
        )
        // Clamps to Feb 28 on non-leap year
        assertTrue(ReminderScheduleEngine.occursOn(monthly31, LocalDate.of(2026, 2, 28)))
        assertFalse(ReminderScheduleEngine.occursOn(monthly31, LocalDate.of(2026, 2, 27)))
    }

    @Test
    fun `nextTriggerDateTime finds next slot today or rolls over to next matching day`() {
        val day = LocalDate.of(2026, 11, 20)
        val reminder = Reminder(
            id = 5L,
            title = "Brush teeth",
            categoryId = 1L,
            isEnabled = true,
            schedule = ReminderSchedule.Daily(intervalDays = 1, startDate = day),
            triggerTimes = listOf(LocalTime.of(7, 30), LocalTime.of(22, 0))
        )

        val midDay = LocalDateTime.of(day, LocalTime.of(12, 0))
        assertEquals(
            LocalDateTime.of(day, LocalTime.of(22, 0)),
            ReminderScheduleEngine.nextTriggerDateTime(reminder, category = healthCategory, after = midDay)
        )

        val lateNight = LocalDateTime.of(day, LocalTime.of(23, 0))
        assertEquals(
            LocalDateTime.of(day.plusDays(1), LocalTime.of(7, 30)),
            ReminderScheduleEngine.nextTriggerDateTime(reminder, category = healthCategory, after = lateNight)
        )

        assertNull(
            ReminderScheduleEngine.nextTriggerDateTime(reminder, category = pausedCategory, after = midDay)
        )
    }

    @Test
    fun `scheduled category activates reminders only within its date range`() {
        val holiday = ReminderCategory(
            id = 3L,
            name = "Holiday",
            colorArgb = 0xFFB45309,
            isEnabled = true,
            activeFrom = LocalDate.of(2026, 12, 20),
            activeUntil = LocalDate.of(2026, 12, 27)
        )
        val reminder = Reminder(
            id = 30L,
            title = "Apply sunscreen",
            categoryId = holiday.id,
            isEnabled = true,
            schedule = ReminderSchedule.Daily(intervalDays = 1, startDate = LocalDate.of(2026, 11, 20)),
            triggerTimes = listOf(LocalTime.of(10, 0))
        )
        val categories = mapOf(holiday.id to holiday)

        // Before range
        assertTrue(
            ReminderScheduleEngine.occurrencesForDate(
                reminders = listOf(reminder),
                categoriesById = categories,
                completions = emptySet(),
                date = LocalDate.of(2026, 12, 19)
            ).isEmpty()
        )
        // Inside range (inclusive bounds)
        assertEquals(
            1,
            ReminderScheduleEngine.occurrencesForDate(
                reminders = listOf(reminder),
                categoriesById = categories,
                completions = emptySet(),
                date = LocalDate.of(2026, 12, 20)
            ).size
        )
        assertEquals(
            1,
            ReminderScheduleEngine.occurrencesForDate(
                reminders = listOf(reminder),
                categoriesById = categories,
                completions = emptySet(),
                date = LocalDate.of(2026, 12, 27)
            ).size
        )
        // After range
        assertTrue(
            ReminderScheduleEngine.occurrencesForDate(
                reminders = listOf(reminder),
                categoriesById = categories,
                completions = emptySet(),
                date = LocalDate.of(2026, 12, 28)
            ).isEmpty()
        )

        // Next trigger jumps straight to activeFrom when queried earlier
        assertEquals(
            LocalDateTime.of(2026, 12, 20, 10, 0),
            ReminderScheduleEngine.nextTriggerDateTime(
                reminder = reminder,
                category = holiday,
                after = LocalDateTime.of(2026, 11, 20, 12, 0)
            )
        )
        // Next trigger is null after activeUntil
        assertNull(
            ReminderScheduleEngine.nextTriggerDateTime(
                reminder = reminder,
                category = holiday,
                after = LocalDateTime.of(2026, 12, 27, 11, 0)
            )
        )
    }

    @Test
    fun `bitmask and trigger times encoding round-trip cleanly`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)
        assertEquals(days, decodeDaysMask(encodeDaysMask(days)))

        val times = listOf(LocalTime.of(22, 15), LocalTime.of(7, 30), LocalTime.of(13, 0))
        assertEquals(times.sorted(), decodeTriggerTimes(encodeTriggerTimes(times)))
    }
}
