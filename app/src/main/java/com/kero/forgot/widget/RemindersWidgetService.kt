package com.kero.forgot.widget

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.kero.forgot.ForgotApplication
import com.kero.forgot.R
import com.kero.forgot.domain.logic.ReminderScheduleEngine
import com.kero.forgot.domain.model.ReminderOccurrence
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RemindersWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        RemindersWidgetViewsFactory(applicationContext)
}

private class RemindersWidgetViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<ReminderOccurrence> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val app = context.applicationContext as? ForgotApplication ?: return
        val today = LocalDate.now()
        items = runBlocking {
            val categoriesById = app.container.categoryRepository.getCategories().associateBy { it.id }
            val reminders = app.container.reminderRepository.getReminders()
            val completions = app.container.reminderRepository.getCompletionsForDate(today)
            ReminderScheduleEngine.occurrencesForDate(
                reminders = reminders,
                categoriesById = categoriesById,
                completions = completions,
                date = today
            )
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val occurrence = items.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.widget_reminder_item)

        views.setTextViewText(R.id.widget_reminder_time, occurrence.time.format(timeFormatter))

        val titleText: CharSequence = if (occurrence.isCompleted) {
            SpannableString(occurrence.title).apply {
                setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            occurrence.title
        }
        views.setTextViewText(R.id.widget_reminder_item_title, titleText)
        views.setTextColor(
            R.id.widget_reminder_item_title,
            ContextCompat.getColor(
                context,
                if (occurrence.isCompleted) R.color.widget_on_surface_variant else R.color.widget_on_surface
            )
        )

        val subtitle = buildString {
            append(occurrence.categoryName)
            if (occurrence.description.isNotBlank()) {
                append(" · ")
                append(occurrence.description)
            }
        }
        views.setTextViewText(R.id.widget_reminder_item_subtitle, subtitle)

        val categoryColor = occurrence.categoryColorArgb.toInt()
        val mutedColor = ContextCompat.getColor(context, R.color.widget_on_surface_variant)
        val tintColor = if (occurrence.isCompleted) mutedColor else categoryColor

        views.setInt(R.id.widget_reminder_color_bar, "setColorFilter", tintColor)
        views.setImageViewResource(
            R.id.widget_reminder_status,
            if (occurrence.isCompleted) {
                R.drawable.ic_widget_check_circle
            } else {
                R.drawable.ic_widget_radio_unchecked
            }
        )
        views.setInt(R.id.widget_reminder_status, "setColorFilter", categoryColor)

        val timeMinutes = occurrence.time.hour * 60 + occurrence.time.minute
        val fillInIntent = Intent().apply {
            putExtra(RemindersWidgetProvider.EXTRA_REMINDER_ID, occurrence.reminderId)
            putExtra(RemindersWidgetProvider.EXTRA_DATE_EPOCH_DAY, occurrence.date.toEpochDay())
            putExtra(RemindersWidgetProvider.EXTRA_TIME_MINUTES, timeMinutes)
            putExtra(RemindersWidgetProvider.EXTRA_NEW_COMPLETED, !occurrence.isCompleted)
        }
        views.setOnClickFillInIntent(R.id.widget_reminder_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        val occurrence = items.getOrNull(position) ?: return position.toLong()
        return (occurrence.reminderId * 1440L) + (occurrence.time.hour * 60 + occurrence.time.minute)
    }

    override fun hasStableIds(): Boolean = true

    companion object {
        private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
