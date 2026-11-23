package com.kero.forgot.di

import android.content.Context
import com.kero.forgot.data.local.ForgotDatabase
import com.kero.forgot.data.local.SeedDataInitializer
import com.kero.forgot.data.repository.RoomCategoryRepository
import com.kero.forgot.data.repository.RoomReminderRepository
import com.kero.forgot.data.repository.RoomTodoRepository
import com.kero.forgot.domain.repository.CategoryRepository
import com.kero.forgot.domain.repository.ReminderRepository
import com.kero.forgot.domain.repository.TodoRepository
import com.kero.forgot.notifications.ReminderScheduler
import com.kero.forgot.widget.WidgetUpdater

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: ForgotDatabase = ForgotDatabase.getInstance(appContext)

    val categoryRepository: CategoryRepository = RoomCategoryRepository(
        categoryDao = database.categoryDao(),
        onDataChanged = { WidgetUpdater.updateReminderWidgets(appContext) }
    )
    val reminderRepository: ReminderRepository = RoomReminderRepository(
        reminderDao = database.reminderDao(),
        completionDao = database.reminderCompletionDao(),
        onDataChanged = { WidgetUpdater.updateReminderWidgets(appContext) }
    )
    val todoRepository: TodoRepository = RoomTodoRepository(
        todoDao = database.todoDao(),
        onDataChanged = { WidgetUpdater.updateTodoWidgets(appContext) }
    )

    val reminderScheduler: ReminderScheduler = ReminderScheduler(
        context = appContext,
        reminderRepository = reminderRepository,
        categoryRepository = categoryRepository
    )

    val seedDataInitializer: SeedDataInitializer = SeedDataInitializer(
        categoryDao = database.categoryDao(),
        reminderDao = database.reminderDao(),
        todoDao = database.todoDao()
    )
}
