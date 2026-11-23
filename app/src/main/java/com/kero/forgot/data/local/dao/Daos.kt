package com.kero.forgot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kero.forgot.data.local.entity.CategoryEntity
import com.kero.forgot.data.local.entity.ReminderCompletionEntity
import com.kero.forgot.data.local.entity.ReminderEntity
import com.kero.forgot.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entity: CategoryEntity): Long

    @Query(
        "UPDATE categories SET isEnabled = :enabled, " +
            "activeFromEpochDay = :activeFromEpochDay, " +
            "activeUntilEpochDay = :activeUntilEpochDay " +
            "WHERE id = :categoryId"
    )
    suspend fun setActivation(
        categoryId: Long,
        enabled: Boolean,
        activeFromEpochDay: Long?,
        activeUntilEpochDay: Long?
    )

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAtEpochMillis ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY createdAtEpochMillis ASC")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Upsert
    suspend fun upsert(entity: ReminderEntity): Long

    @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :reminderId")
    suspend fun setEnabled(reminderId: Long, enabled: Boolean)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteById(reminderId: Long)
}

@Dao
interface ReminderCompletionDao {
    @Query(
        "SELECT * FROM reminder_completions " +
            "WHERE dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay"
    )
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ReminderCompletionEntity>>

    @Query("SELECT * FROM reminder_completions WHERE dateEpochDay = :dateEpochDay")
    suspend fun getForDate(dateEpochDay: Long): List<ReminderCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReminderCompletionEntity)

    @Query(
        "DELETE FROM reminder_completions " +
            "WHERE reminderId = :reminderId AND dateEpochDay = :dateEpochDay AND timeMinutes = :timeMinutes"
    )
    suspend fun delete(reminderId: Long, dateEpochDay: Long, timeMinutes: Int)
}

@Dao
interface TodoDao {
    @Query(
        "SELECT * FROM todos " +
            "WHERE isArchived = 0 AND targetDateEpochDay >= :startEpochDay AND targetDateEpochDay <= :endEpochDay " +
            "ORDER BY isCompleted ASC, sortOrder ASC, createdAtEpochMillis ASC"
    )
    fun observeActiveInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TodoEntity>>

    @Query(
        "SELECT * FROM todos " +
            "WHERE isArchived = 0 AND targetDateEpochDay = :dateEpochDay " +
            "ORDER BY isCompleted ASC, sortOrder ASC, createdAtEpochMillis ASC"
    )
    suspend fun getActiveForDate(dateEpochDay: Long): List<TodoEntity>

    @Query(
        "SELECT * FROM todos " +
            "WHERE isArchived = 0 AND isCompleted = 0 AND targetDateEpochDay < :todayEpochDay " +
            "ORDER BY targetDateEpochDay ASC, createdAtEpochMillis ASC"
    )
    fun observePendingPast(todayEpochDay: Long): Flow<List<TodoEntity>>

    @Query(
        "SELECT * FROM todos " +
            "WHERE isArchived = 1 " +
            "ORDER BY targetDateEpochDay DESC, createdAtEpochMillis DESC"
    )
    fun observeArchived(): Flow<List<TodoEntity>>

    @Upsert
    suspend fun upsert(entity: TodoEntity): Long

    @Query("UPDATE todos SET isCompleted = :completed WHERE id = :todoId")
    suspend fun setCompleted(todoId: Long, completed: Boolean)

    @Query("UPDATE todos SET targetDateEpochDay = :newEpochDay WHERE id = :todoId")
    suspend fun moveToDate(todoId: Long, newEpochDay: Long)

    @Query("UPDATE todos SET targetDateEpochDay = :newEpochDay WHERE id IN (:todoIds)")
    suspend fun moveManyToDate(todoIds: List<Long>, newEpochDay: Long)

    @Query("UPDATE todos SET isArchived = 1 WHERE id = :todoId")
    suspend fun archive(todoId: Long)

    @Query("UPDATE todos SET isArchived = 1 WHERE id IN (:todoIds)")
    suspend fun archiveMany(todoIds: List<Long>)

    @Query("UPDATE todos SET isArchived = 0, isCompleted = 0, targetDateEpochDay = :targetEpochDay WHERE id = :todoId")
    suspend fun restore(todoId: Long, targetEpochDay: Long)

    @Query("DELETE FROM todos WHERE id = :todoId")
    suspend fun deleteById(todoId: Long)
}
