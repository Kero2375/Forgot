package com.kero.forgot.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kero.forgot.domain.logic.ReminderScheduleEngine
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.repository.CategoryRepository
import com.kero.forgot.domain.repository.ReminderRepository
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(
    private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val categoryRepository: CategoryRepository
) {
    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    suspend fun syncAll() {
        val categories = categoryRepository.getCategories().associateBy { it.id }
        val reminders = reminderRepository.getReminders()
        val now = LocalDateTime.now()
        for (reminder in reminders) {
            val category = categories[reminder.categoryId]
            scheduleReminder(reminder, category, now)
        }
    }

    suspend fun syncReminder(reminderId: Long) {
        val reminder = reminderRepository.getReminderById(reminderId)
        if (reminder == null) {
            cancelReminder(reminderId)
            return
        }
        val categories = categoryRepository.getCategories().associateBy { it.id }
        val category = categories[reminder.categoryId]
        scheduleReminder(reminder, category, LocalDateTime.now())
    }

    fun scheduleReminder(
        reminder: Reminder,
        category: ReminderCategory?,
        after: LocalDateTime = LocalDateTime.now()
    ) {
        val manager = alarmManager ?: return
        val nextDateTime = ReminderScheduleEngine.nextTriggerDateTime(
            reminder = reminder,
            category = category,
            after = after
        )

        if (nextDateTime == null) {
            cancelReminder(reminder.id)
            return
        }

        val triggerMillis = nextDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val pendingIntent = buildPendingIntent(
            reminderId = reminder.id,
            dateEpochDay = nextDateTime.toLocalDate().toEpochDay(),
            timeMinutes = nextDateTime.hour * 60 + nextDateTime.minute
        )

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()

        if (canScheduleExact) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminderId: Long) {
        val manager = alarmManager ?: return
        val pendingIntent = buildPendingIntent(
            reminderId = reminderId,
            dateEpochDay = 0L,
            timeMinutes = 0
        )
        manager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(
        reminderId: Long,
        dateEpochDay: Long,
        timeMinutes: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_DATE_EPOCH_DAY, dateEpochDay)
            putExtra(ReminderAlarmReceiver.EXTRA_TIME_MINUTES, timeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
