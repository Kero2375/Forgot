package com.kero.forgot.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kero.forgot.data.local.dao.CategoryDao
import com.kero.forgot.data.local.dao.ReminderCompletionDao
import com.kero.forgot.data.local.dao.ReminderDao
import com.kero.forgot.data.local.dao.TodoDao
import com.kero.forgot.data.local.entity.CategoryEntity
import com.kero.forgot.data.local.entity.ReminderCompletionEntity
import com.kero.forgot.data.local.entity.ReminderEntity
import com.kero.forgot.data.local.entity.TodoEntity

@Database(
    entities = [
        CategoryEntity::class,
        ReminderEntity::class,
        ReminderCompletionEntity::class,
        TodoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ForgotDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderCompletionDao(): ReminderCompletionDao
    abstract fun todoDao(): TodoDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN activeFromEpochDay INTEGER")
                db.execSQL("ALTER TABLE categories ADD COLUMN activeUntilEpochDay INTEGER")
            }
        }

        @Volatile
        private var instance: ForgotDatabase? = null

        fun getInstance(context: Context): ForgotDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ForgotDatabase::class.java,
                    "forgot.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
