package com.kero.forgot.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.kero.forgot.ForgotApplication
import com.kero.forgot.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? ForgotApplication ?: return
        val container = app.container
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TRIGGER_REMINDER -> {
                        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        val dateEpochDay = intent.getLongExtra(EXTRA_DATE_EPOCH_DAY, LocalDate.now().toEpochDay())
                        val timeMinutes = intent.getIntExtra(EXTRA_TIME_MINUTES, 0)
                        if (reminderId > 0L) {
                            val reminder = container.reminderRepository.getReminderById(reminderId)
                            val categories = container.categoryRepository.getCategories().associateBy { it.id }
                            if (reminder != null) {
                                val triggerDate = LocalDate.ofEpochDay(dateEpochDay)
                                val category = categories[reminder.categoryId]
                                if (reminder.isEnabled && (category?.isActiveOn(triggerDate) != false)) {
                                    NotificationHelper.showReminderNotification(
                                        context = context,
                                        reminderId = reminder.id,
                                        title = reminder.title,
                                        description = reminder.description,
                                        categoryName = category?.name ?: "Reminder",
                                        date = triggerDate,
                                        time = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
                                    )
                                }
                                container.reminderScheduler.syncReminder(reminder.id)
                            }
                        }
                    }

                    ACTION_MARK_DONE -> {
                        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        val dateEpochDay = intent.getLongExtra(EXTRA_DATE_EPOCH_DAY, LocalDate.now().toEpochDay())
                        val timeMinutes = intent.getIntExtra(EXTRA_TIME_MINUTES, 0)
                        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                        if (reminderId > 0L) {
                            container.reminderRepository.toggleOccurrenceCompletion(
                                reminderId = reminderId,
                                date = LocalDate.ofEpochDay(dateEpochDay),
                                time = LocalTime.of(timeMinutes / 60, timeMinutes % 60),
                                completed = true
                            )
                        }
                        if (notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                    }

                    Intent.ACTION_BOOT_COMPLETED -> {
                        container.reminderScheduler.syncAll()
                        WidgetUpdater.updateAllWidgets(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.kero.forgot.ACTION_TRIGGER_REMINDER"
        const val ACTION_MARK_DONE = "com.kero.forgot.ACTION_MARK_DONE"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_DATE_EPOCH_DAY = "extra_date_epoch_day"
        const val EXTRA_TIME_MINUTES = "extra_time_minutes"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
