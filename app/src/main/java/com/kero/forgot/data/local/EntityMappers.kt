package com.kero.forgot.data.local

import com.kero.forgot.data.local.entity.CategoryEntity
import com.kero.forgot.data.local.entity.ReminderCompletionEntity
import com.kero.forgot.data.local.entity.ReminderEntity
import com.kero.forgot.data.local.entity.TodoEntity
import com.kero.forgot.domain.model.CompletionKey
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.ReminderSchedule
import com.kero.forgot.domain.model.TodoItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private const val TYPE_ONE_SHOT = "ONE_SHOT"
private const val TYPE_DAILY = "DAILY"
private const val TYPE_WEEKLY = "WEEKLY"
private const val TYPE_MONTHLY = "MONTHLY"

fun CategoryEntity.toDomain(): ReminderCategory = ReminderCategory(
    id = id,
    name = name,
    colorArgb = colorArgb,
    isEnabled = isEnabled,
    sortOrder = sortOrder,
    activeFrom = activeFromEpochDay?.let(LocalDate::ofEpochDay),
    activeUntil = activeUntilEpochDay?.let(LocalDate::ofEpochDay)
)

fun ReminderCategory.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorArgb = colorArgb,
    isEnabled = isEnabled,
    sortOrder = sortOrder,
    activeFromEpochDay = activeFrom?.toEpochDay(),
    activeUntilEpochDay = activeUntil?.toEpochDay()
)

fun ReminderEntity.toDomain(): Reminder {
    val anchorDate = LocalDate.ofEpochDay(anchorDateEpochDay)
    val schedule = when (scheduleType) {
        TYPE_ONE_SHOT -> ReminderSchedule.OneShot(date = anchorDate)
        TYPE_DAILY -> ReminderSchedule.Daily(
            intervalDays = intervalDays.coerceAtLeast(1),
            startDate = anchorDate
        )
        TYPE_WEEKLY -> ReminderSchedule.Weekly(
            daysOfWeek = decodeDaysMask(daysOfWeekMask),
            startDate = anchorDate
        )
        TYPE_MONTHLY -> ReminderSchedule.Monthly(
            dayOfMonth = dayOfMonth.coerceIn(1, 31),
            startDate = anchorDate
        )
        else -> ReminderSchedule.Daily(intervalDays = 1, startDate = anchorDate)
    }

    return Reminder(
        id = id,
        title = title,
        description = description,
        categoryId = categoryId,
        isEnabled = isEnabled,
        schedule = schedule,
        triggerTimes = decodeTriggerTimes(triggerTimesMinutes),
        createdAtEpochMillis = createdAtEpochMillis
    )
}

fun Reminder.toEntity(): ReminderEntity {
    val scheduleType: String
    val anchorDate: LocalDate
    var intervalDays = 1
    var daysMask = 0
    var dayOfMonth = 1

    when (val s = schedule) {
        is ReminderSchedule.OneShot -> {
            scheduleType = TYPE_ONE_SHOT
            anchorDate = s.date
        }
        is ReminderSchedule.Daily -> {
            scheduleType = TYPE_DAILY
            anchorDate = s.startDate
            intervalDays = s.intervalDays
        }
        is ReminderSchedule.Weekly -> {
            scheduleType = TYPE_WEEKLY
            anchorDate = s.startDate
            daysMask = encodeDaysMask(s.daysOfWeek)
        }
        is ReminderSchedule.Monthly -> {
            scheduleType = TYPE_MONTHLY
            anchorDate = s.startDate
            dayOfMonth = s.dayOfMonth
        }
    }

    return ReminderEntity(
        id = id,
        title = title,
        description = description,
        categoryId = categoryId,
        isEnabled = isEnabled,
        scheduleType = scheduleType,
        anchorDateEpochDay = anchorDate.toEpochDay(),
        intervalDays = intervalDays,
        daysOfWeekMask = daysMask,
        dayOfMonth = dayOfMonth,
        triggerTimesMinutes = encodeTriggerTimes(triggerTimes),
        createdAtEpochMillis = createdAtEpochMillis
    )
}

fun ReminderCompletionEntity.toDomain(): CompletionKey = CompletionKey(
    reminderId = reminderId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    time = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
)

fun TodoEntity.toDomain(): TodoItem = TodoItem(
    id = id,
    title = title,
    notes = notes,
    targetDate = LocalDate.ofEpochDay(targetDateEpochDay),
    isCompleted = isCompleted,
    isArchived = isArchived,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAtEpochMillis
)

fun TodoItem.toEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    notes = notes,
    targetDateEpochDay = targetDate.toEpochDay(),
    isCompleted = isCompleted,
    isArchived = isArchived,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAtEpochMillis
)

fun encodeDaysMask(days: Set<DayOfWeek>): Int {
    var mask = 0
    for (day in days) {
        mask = mask or (1 shl (day.value - 1))
    }
    return mask
}

fun decodeDaysMask(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(linkedSetOf()) { day ->
        (mask and (1 shl (day.value - 1))) != 0
    }

fun encodeTriggerTimes(times: List<LocalTime>): String =
    times.distinct()
        .sorted()
        .joinToString(",") { (it.hour * 60 + it.minute).toString() }

fun decodeTriggerTimes(encoded: String): List<LocalTime> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(",")
        .mapNotNull { token -> token.trim().toIntOrNull() }
        .filter { it in 0 until 24 * 60 }
        .distinct()
        .sorted()
        .map { LocalTime.of(it / 60, it % 60) }
}
