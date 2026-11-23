package com.kero.forgot.domain.logic

import com.kero.forgot.domain.model.CompletionKey
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.ReminderOccurrence
import com.kero.forgot.domain.model.ReminderSchedule
import com.kero.forgot.domain.model.TimelineItem
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object ReminderScheduleEngine {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

    fun occursOn(reminder: Reminder, date: LocalDate): Boolean {
        if (reminder.triggerTimes.isEmpty()) return false
        return when (val schedule = reminder.schedule) {
            is ReminderSchedule.OneShot -> schedule.date == date
            is ReminderSchedule.Daily -> {
                if (date.isBefore(schedule.startDate)) return false
                val daysBetween = ChronoUnit.DAYS.between(schedule.startDate, date)
                daysBetween % schedule.intervalDays == 0L
            }
            is ReminderSchedule.Weekly -> {
                if (date.isBefore(schedule.startDate)) return false
                date.dayOfWeek in schedule.daysOfWeek
            }
            is ReminderSchedule.Monthly -> {
                if (date.isBefore(schedule.startDate)) return false
                val effectiveDay = minOf(schedule.dayOfMonth, date.lengthOfMonth())
                date.dayOfMonth == effectiveDay
            }
        }
    }

    fun occurrencesForDate(
        reminders: List<Reminder>,
        categoriesById: Map<Long, ReminderCategory>,
        completions: Set<CompletionKey>,
        date: LocalDate
    ): List<ReminderOccurrence> {
        return reminders
            .asSequence()
            .filter { reminder ->
                val category = categoriesById[reminder.categoryId]
                reminder.isEnabled && (category?.isActiveOn(date) != false) && occursOn(reminder, date)
            }
            .flatMap { reminder ->
                val category = categoriesById[reminder.categoryId]
                reminder.triggerTimes
                    .distinct()
                    .sorted()
                    .asSequence()
                    .map { time ->
                        val key = CompletionKey(reminder.id, date, time)
                        ReminderOccurrence(
                            reminderId = reminder.id,
                            title = reminder.title,
                            description = reminder.description,
                            categoryId = reminder.categoryId,
                            categoryName = category?.name ?: "General",
                            categoryColorArgb = category?.colorArgb ?: 0xFF4A6572,
                            date = date,
                            time = time,
                            isCompleted = key in completions,
                            isPeriodic = reminder.schedule.isPeriodic
                        )
                    }
            }
            .sortedWith(compareBy<ReminderOccurrence> { it.time }.thenBy { it.title.lowercase() })
            .toList()
    }

    fun buildTimeline(
        occurrences: List<ReminderOccurrence>,
        gapThresholdMinutes: Long = 60L
    ): List<TimelineItem> {
        if (occurrences.isEmpty()) return emptyList()

        val groupedSlots = occurrences
            .groupBy { it.time }
            .toSortedMap()
            .map { (time, items) -> TimelineItem.TimeSlot(time = time, occurrences = items) }

        val result = mutableListOf<TimelineItem>()
        groupedSlots.forEachIndexed { index, slot ->
            if (index > 0) {
                val previousTime = groupedSlots[index - 1].time
                val gapMinutes = Duration.between(previousTime, slot.time).toMinutes()
                if (gapMinutes >= gapThresholdMinutes) {
                    result += TimelineItem.TimeGap(
                        startTime = previousTime,
                        endTime = slot.time,
                        durationMinutes = gapMinutes
                    )
                }
            }
            result += slot
        }
        return result
    }

    fun nextTriggerDateTime(
        reminder: Reminder,
        category: ReminderCategory?,
        after: LocalDateTime
    ): LocalDateTime? {
        if (!reminder.isEnabled || (category != null && !category.isEnabled) || reminder.triggerTimes.isEmpty()) {
            return null
        }
        val sortedTimes = reminder.triggerTimes.distinct().sorted()
        val startSearchDate = after.toLocalDate()

        if (reminder.schedule is ReminderSchedule.OneShot) {
            val targetDate = reminder.schedule.date
            if (targetDate.isBefore(startSearchDate)) return null
            if (category != null && !category.isActiveOn(targetDate)) return null
            for (time in sortedTimes) {
                val candidate = LocalDateTime.of(targetDate, time)
                if (candidate.isAfter(after)) return candidate
            }
            return null
        }

        // If category has a future activeFrom date, jump straight there to avoid missing ranges > 400 days
        val effectiveStart = if (category?.activeFrom != null && category.activeFrom.isAfter(startSearchDate)) {
            category.activeFrom
        } else {
            startSearchDate
        }

        // Search up to 400 days ahead from effective start
        for (offset in 0L..400L) {
            val candidateDate = effectiveStart.plusDays(offset)
            if (category?.activeUntil != null && candidateDate.isAfter(category.activeUntil)) {
                return null
            }
            if ((category?.isActiveOn(candidateDate) != false) && occursOn(reminder, candidateDate)) {
                for (time in sortedTimes) {
                    val candidate = LocalDateTime.of(candidateDate, time)
                    if (candidate.isAfter(after)) return candidate
                }
            }
        }
        return null
    }

    fun formatCategorySchedule(category: ReminderCategory): String? {
        val from = category.activeFrom
        val until = category.activeUntil
        return when {
            from != null && until != null -> "${from.format(shortDateFormatter)} – ${until.format(shortDateFormatter)}"
            from != null -> "From ${from.format(shortDateFormatter)}"
            until != null -> "Until ${until.format(shortDateFormatter)}"
            else -> null
        }
    }

    fun formatDuration(minutes: Long): String {
        val hours = minutes / 60
        val remMinutes = minutes % 60
        return when {
            hours > 0 && remMinutes > 0 -> "${hours}h ${remMinutes}m"
            hours > 0 -> "${hours}h"
            else -> "${remMinutes}m"
        }
    }

    fun formatScheduleSummary(reminder: Reminder): String {
        val schedulePart = when (val s = reminder.schedule) {
            is ReminderSchedule.OneShot -> "Once · ${s.date.format(shortDateFormatter)}"
            is ReminderSchedule.Daily -> if (s.intervalDays == 1) "Every day" else "Every ${s.intervalDays} days"
            is ReminderSchedule.Weekly -> {
                if (s.daysOfWeek.size == 7) {
                    "Every day"
                } else if (s.daysOfWeek == setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                ) {
                    "Weekdays"
                } else if (s.daysOfWeek == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                    "Weekends"
                } else {
                    s.daysOfWeek
                        .sortedBy { it.value }
                        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
                }
            }
            is ReminderSchedule.Monthly -> "Monthly · Day ${s.dayOfMonth}"
        }

        val timesPart = reminder.triggerTimes
            .distinct()
            .sorted()
            .joinToString(", ") { it.format(timeFormatter) }

        return if (timesPart.isBlank()) schedulePart else "$schedulePart · $timesPart"
    }
}
