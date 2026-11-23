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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodoListWidgetProvider : AppWidgetProvider() {

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
            ACTION_TOGGLE_TODO -> {
                val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
                val newCompleted = intent.getBooleanExtra(EXTRA_NEW_COMPLETED, false)
                if (todoId > 0L) {
                    val app = context.applicationContext as? ForgotApplication ?: return
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            app.container.todoRepository.setTodoCompleted(todoId, newCompleted)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                WidgetUpdater.updateTodoWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TODO = "com.kero.forgot.widget.ACTION_TOGGLE_TODO"
        const val EXTRA_TODO_ID = "extra_todo_id"
        const val EXTRA_NEW_COMPLETED = "extra_new_completed"

        @Suppress("DEPRECATION")
        private suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val app = context.applicationContext as? ForgotApplication ?: return
            val today = LocalDate.now()
            val todos = app.container.todoRepository.getActiveTodosForDate(today)
            val totalCount = todos.size
            val completedCount = todos.count { it.isCompleted }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_HOME)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_TODO,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val toggleTemplateIntent = Intent(context, TodoListWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TODO
            }
            val toggleTemplatePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_TOGGLE_TODO,
                toggleTemplateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_todo_list)

                if (totalCount > 0) {
                    views.setViewVisibility(R.id.widget_todo_count, View.VISIBLE)
                    views.setTextViewText(R.id.widget_todo_count, "$completedCount/$totalCount")
                } else {
                    views.setViewVisibility(R.id.widget_todo_count, View.GONE)
                }

                views.setOnClickPendingIntent(R.id.widget_todo_header, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_todo_empty, openAppPendingIntent)

                val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_todo_list, serviceIntent)
                views.setEmptyView(R.id.widget_todo_list, R.id.widget_todo_empty)
                views.setPendingIntentTemplate(R.id.widget_todo_list, toggleTemplatePendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private const val REQUEST_CODE_OPEN_TODO = 4001
        private const val REQUEST_CODE_TOGGLE_TODO = 4002
    }
}
