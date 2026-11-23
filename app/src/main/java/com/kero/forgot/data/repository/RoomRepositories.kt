package com.kero.forgot.data.repository

import com.kero.forgot.data.local.dao.CategoryDao
import com.kero.forgot.data.local.dao.ReminderCompletionDao
import com.kero.forgot.data.local.dao.ReminderDao
import com.kero.forgot.data.local.dao.TodoDao
import com.kero.forgot.data.local.entity.ReminderCompletionEntity
import com.kero.forgot.data.local.toDomain
import com.kero.forgot.data.local.toEntity
import com.kero.forgot.domain.model.CompletionKey
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.TodoItem
import com.kero.forgot.domain.repository.CategoryRepository
import com.kero.forgot.domain.repository.ReminderRepository
import com.kero.forgot.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
    private val onDataChanged: () -> Unit = {}
) : CategoryRepository {
    override fun observeCategories(): Flow<List<ReminderCategory>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCategories(): List<ReminderCategory> =
        categoryDao.getAll().map { it.toDomain() }

    override suspend fun upsertCategory(category: ReminderCategory): Long =
        categoryDao.upsert(category.toEntity()).also { onDataChanged() }

    override suspend fun setCategoryActivation(
        categoryId: Long,
        enabled: Boolean,
        activeFrom: LocalDate?,
        activeUntil: LocalDate?
    ) {
        categoryDao.setActivation(
            categoryId = categoryId,
            enabled = enabled,
            activeFromEpochDay = activeFrom?.toEpochDay(),
            activeUntilEpochDay = activeUntil?.toEpochDay()
        )
        onDataChanged()
    }

    override suspend fun deleteCategory(categoryId: Long) {
        categoryDao.deleteById(categoryId)
        onDataChanged()
    }
}

class RoomReminderRepository(
    private val reminderDao: ReminderDao,
    private val completionDao: ReminderCompletionDao,
    private val onDataChanged: () -> Unit = {}
) : ReminderRepository {
    override fun observeReminders(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getReminders(): List<Reminder> =
        reminderDao.getAll().map { it.toDomain() }

    override suspend fun getReminderById(id: Long): Reminder? =
        reminderDao.getById(id)?.toDomain()

    override fun observeCompletionsInRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Set<CompletionKey>> =
        completionDao.observeInRange(startDate.toEpochDay(), endDate.toEpochDay())
            .map { list -> list.mapTo(hashSetOf()) { it.toDomain() } }

    override suspend fun getCompletionsForDate(date: LocalDate): Set<CompletionKey> =
        completionDao.getForDate(date.toEpochDay()).mapTo(hashSetOf()) { it.toDomain() }

    override suspend fun upsertReminder(reminder: Reminder): Long =
        reminderDao.upsert(reminder.toEntity()).also { onDataChanged() }

    override suspend fun setReminderEnabled(reminderId: Long, enabled: Boolean) {
        reminderDao.setEnabled(reminderId, enabled)
        onDataChanged()
    }

    override suspend fun deleteReminder(reminderId: Long) {
        reminderDao.deleteById(reminderId)
        onDataChanged()
    }

    override suspend fun toggleOccurrenceCompletion(
        reminderId: Long,
        date: LocalDate,
        time: LocalTime,
        completed: Boolean
    ) {
        val timeMinutes = time.hour * 60 + time.minute
        if (completed) {
            completionDao.insert(
                ReminderCompletionEntity(
                    reminderId = reminderId,
                    dateEpochDay = date.toEpochDay(),
                    timeMinutes = timeMinutes,
                    completedAtEpochMillis = System.currentTimeMillis()
                )
            )
        } else {
            completionDao.delete(
                reminderId = reminderId,
                dateEpochDay = date.toEpochDay(),
                timeMinutes = timeMinutes
            )
        }
        onDataChanged()
    }
}

class RoomTodoRepository(
    private val todoDao: TodoDao,
    private val onDataChanged: () -> Unit = {}
) : TodoRepository {
    override fun observeActiveTodosInRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TodoItem>> =
        todoDao.observeActiveInRange(startDate.toEpochDay(), endDate.toEpochDay())
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveTodosForDate(date: LocalDate): List<TodoItem> =
        todoDao.getActiveForDate(date.toEpochDay()).map { it.toDomain() }

    override fun observePendingPastTodos(today: LocalDate): Flow<List<TodoItem>> =
        todoDao.observePendingPast(today.toEpochDay())
            .map { list -> list.map { it.toDomain() } }

    override fun observeArchivedTodos(): Flow<List<TodoItem>> =
        todoDao.observeArchived().map { list -> list.map { it.toDomain() } }

    override suspend fun addTodo(title: String, notes: String, targetDate: LocalDate): Long =
        todoDao.upsert(
            TodoItem(
                title = title.trim(),
                notes = notes.trim(),
                targetDate = targetDate
            ).toEntity()
        ).also { onDataChanged() }

    override suspend fun updateTodo(todo: TodoItem) {
        todoDao.upsert(todo.toEntity())
        onDataChanged()
    }

    override suspend fun setTodoCompleted(todoId: Long, completed: Boolean) {
        todoDao.setCompleted(todoId, completed)
        onDataChanged()
    }

    override suspend fun moveTodoToDate(todoId: Long, newDate: LocalDate) {
        todoDao.moveToDate(todoId, newDate.toEpochDay())
        onDataChanged()
    }

    override suspend fun moveTodosToDate(todoIds: List<Long>, newDate: LocalDate) {
        if (todoIds.isEmpty()) return
        todoDao.moveManyToDate(todoIds, newDate.toEpochDay())
        onDataChanged()
    }

    override suspend fun archiveTodo(todoId: Long) {
        todoDao.archive(todoId)
        onDataChanged()
    }

    override suspend fun archiveTodos(todoIds: List<Long>) {
        if (todoIds.isEmpty()) return
        todoDao.archiveMany(todoIds)
        onDataChanged()
    }

    override suspend fun restoreTodo(todoId: Long, targetDate: LocalDate) {
        todoDao.restore(todoId, targetDate.toEpochDay())
        onDataChanged()
    }

    override suspend fun deleteTodo(todoId: Long) {
        todoDao.deleteById(todoId)
        onDataChanged()
    }
}
