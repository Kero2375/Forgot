package com.kero.forgot.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.kero.forgot.R

object WidgetUpdater {

    @Suppress("DEPRECATION")
    fun updateTodoWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, TodoListWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_todo_list)
        val intent = Intent(appContext, TodoListWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        appContext.sendBroadcast(intent)
    }

    @Suppress("DEPRECATION")
    fun updateReminderWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, RemindersWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_reminders_list)
        val intent = Intent(appContext, RemindersWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        appContext.sendBroadcast(intent)
    }

    fun updateAllWidgets(context: Context) {
        updateTodoWidgets(context)
        updateReminderWidgets(context)
    }
}
