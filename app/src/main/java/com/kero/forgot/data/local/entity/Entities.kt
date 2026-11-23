package com.kero.forgot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorArgb: Long,
    val isEnabled: Boolean,
    val sortOrder: Int,
    val activeFromEpochDay: Long? = null,
    val activeUntilEpochDay: Long? = null
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val categoryId: Long,
    val isEnabled: Boolean,
    val scheduleType: String,
    val anchorDateEpochDay: Long,
    val intervalDays: Int,
    val daysOfWeekMask: Int,
    val dayOfMonth: Int,
    val triggerTimesMinutes: String,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "reminder_completions",
    primaryKeys = ["reminderId", "dateEpochDay", "timeMinutes"],
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dateEpochDay")]
)
data class ReminderCompletionEntity(
    val reminderId: Long,
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val completedAtEpochMillis: Long
)

@Entity(
    tableName = "todos",
    indices = [Index("targetDateEpochDay"), Index("isArchived")]
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val notes: String,
    val targetDateEpochDay: Long,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val sortOrder: Int,
    val createdAtEpochMillis: Long
)
