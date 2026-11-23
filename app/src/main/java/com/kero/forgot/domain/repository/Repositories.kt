package com.kero.forgot.domain.repository

import com.kero.forgot.domain.model.CompletionKey
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

interface CategoryRepository {
    fun observeCategories(): Flow<List<ReminderCategory>>
    suspend fun getCategories(): List<ReminderCategory>
    suspend fun upsertCategory(category: ReminderCategory): Long
    suspend fun setCategoryActivation(
        categoryId: Long,
        enabled: Boolean,
        activeFrom: LocalDate? = null,
        activeUntil: LocalDate? = null
    )
    suspend fun deleteCategory(categoryId: Long)
}

interface ReminderRepository {
    fun observeReminders(): Flow<List<Reminder>>
    suspend fun getReminders(): List<Reminder>
    suspend fun getReminderById(id: Long): Reminder?
    fun observeCompletionsInRange(startDate: LocalDate, endDate: LocalDate): Flow<Set<CompletionKey>>
    suspend fun getCompletionsForDate(date: LocalDate): Set<CompletionKey>
    suspend fun upsertReminder(reminder: Reminder): Long
    suspend fun setReminderEnabled(reminderId: Long, enabled: Boolean)
    suspend fun deleteReminder(reminderId: Long)
    suspend fun toggleOccurrenceCompletion(reminderId: Long, date: LocalDate, time: LocalTime, completed: Boolean)
}

interface TodoRepository {
    fun observeActiveTodosInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TodoItem>>
    suspend fun getActiveTodosForDate(date: LocalDate): List<TodoItem>
    fun observePendingPastTodos(today: LocalDate): Flow<List<TodoItem>>
    fun observeArchivedTodos(): Flow<List<TodoItem>>
    suspend fun addTodo(title: String, notes: String, targetDate: LocalDate): Long
    suspend fun updateTodo(todo: TodoItem)
    suspend fun setTodoCompleted(todoId: Long, completed: Boolean)
    suspend fun moveTodoToDate(todoId: Long, newDate: LocalDate)
    suspend fun moveTodosToDate(todoIds: List<Long>, newDate: LocalDate)
    suspend fun archiveTodo(todoId: Long)
    suspend fun archiveTodos(todoIds: List<Long>)
    suspend fun restoreTodo(todoId: Long, targetDate: LocalDate)
    suspend fun deleteTodo(todoId: Long)
}
