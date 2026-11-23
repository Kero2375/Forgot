package com.kero.forgot.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.kero.forgot.ForgotApplication
import com.kero.forgot.MainActivity
import com.kero.forgot.R
import com.kero.forgot.domain.logic.ReminderScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class RemindersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_REMINDER -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                val dateEpochDay = intent.getLongExtra(EXTRA_DATE_EPOCH_DAY, LocalDate.now().toEpochDay())
                val timeMinutes = intent.getIntExtra(EXTRA_TIME_MINUTES, -1)
                val newCompleted = intent.getBooleanExtra(EXTRA_NEW_COMPLETED, false)
                if (reminderId > 0L && timeMinutes >= 0) {
                    val app = context.applicationContext as? ForgotApplication ?: return
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            app.container.reminderRepository.toggleOccurrenceCompletion(
                                reminderId = reminderId,
                                date = LocalDate.ofEpochDay(dateEpochDay),
                                time = LocalTime.of(timeMinutes / 60, timeMinutes % 60),
                                completed = newCompleted
                            )
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                WidgetUpdater.updateReminderWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_REMINDER = "com.kero.forgot.widget.ACTION_TOGGLE_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_DATE_EPOCH_DAY = "extra_date_epoch_day"
        const val EXTRA_TIME_MINUTES = "extra_time_minutes"
        const val EXTRA_NEW_COMPLETED = "extra_new_completed"

        @Suppress("DEPRECATION")
        private suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val app = context.applicationContext as? ForgotApplication ?: return
            val today = LocalDate.now()
            val categoriesById = app.container.categoryRepository.getCategories().associateBy { it.id }
            val reminders = app.container.reminderRepository.getReminders()
            val completions = app.container.reminderRepository.getCompletionsForDate(today)
            val occurrences = ReminderScheduleEngine.occurrencesForDate(
                reminders = reminders,
                categoriesById = categoriesById,
                completions = completions,
                date = today
            )
            val totalCount = occurrences.size
            val completedCount = occurrences.count { it.isCompleted }

            val openRemindersIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_REMINDERS)
            }
            val openRemindersPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_REMINDERS,
                openRemindersIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val toggleTemplateIntent = Intent(context, RemindersWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_REMINDER
            }
            val toggleTemplatePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_TOGGLE_REMINDER,
                toggleTemplateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_reminders)

                if (totalCount > 0) {
                    views.setViewVisibility(R.id.widget_reminders_count, View.VISIBLE)
                    views.setTextViewText(R.id.widget_reminders_count, "$completedCount/$totalCount")
                } else {
                    views.setViewVisibility(R.id.widget_reminders_count, View.GONE)
                }

                views.setOnClickPendingIntent(R.id.widget_reminders_header, openRemindersPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_reminders_empty, openRemindersPendingIntent)

                val serviceIntent = Intent(context, RemindersWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_reminders_list, serviceIntent)
                views.setEmptyView(R.id.widget_reminders_list, R.id.widget_reminders_empty)
                views.setPendingIntentTemplate(R.id.widget_reminders_list, toggleTemplatePendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private const val REQUEST_CODE_OPEN_REMINDERS = 4101
        private const val REQUEST_CODE_TOGGLE_REMINDER = 4102
    }
}
