package com.kero.forgot.widget

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.kero.forgot.ForgotApplication
import com.kero.forgot.R
import com.kero.forgot.domain.model.TodoItem
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodoWidgetViewsFactory(applicationContext)
}

private class TodoWidgetViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TodoItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val app = context.applicationContext as? ForgotApplication ?: return
        items = runBlocking {
            app.container.todoRepository.getActiveTodosForDate(LocalDate.now())
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val todo = items.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.widget_todo_item)

        val titleText: CharSequence = if (todo.isCompleted) {
            SpannableString(todo.title).apply {
                setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            todo.title
        }

        views.setTextViewText(R.id.widget_todo_item_title, titleText)
        views.setTextColor(
            R.id.widget_todo_item_title,
            ContextCompat.getColor(
                context,
                if (todo.isCompleted) R.color.widget_on_surface_variant else R.color.widget_on_surface
            )
        )

        if (todo.notes.isNotBlank()) {
            views.setViewVisibility(R.id.widget_todo_item_notes, View.VISIBLE)
            views.setTextViewText(R.id.widget_todo_item_notes, todo.notes)
        } else {
            views.setViewVisibility(R.id.widget_todo_item_notes, View.GONE)
        }

        views.setImageViewResource(
            R.id.widget_todo_checkbox,
            if (todo.isCompleted) {
                R.drawable.ic_widget_check_box
            } else {
                R.drawable.ic_widget_check_box_outline
            }
        )

        val fillInIntent = Intent().apply {
            putExtra(TodoListWidgetProvider.EXTRA_TODO_ID, todo.id)
            putExtra(TodoListWidgetProvider.EXTRA_NEW_COMPLETED, !todo.isCompleted)
        }
        views.setOnClickFillInIntent(R.id.widget_todo_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
