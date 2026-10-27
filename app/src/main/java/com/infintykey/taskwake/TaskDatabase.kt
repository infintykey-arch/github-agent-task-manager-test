package com.infintykey.taskwake

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, reminderAtMillis IS NULL ASC, reminderAtMillis ASC, id DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderAtMillis IS NOT NULL AND reminderAtMillis > :nowMillis")
    suspend fun getFutureReminders(nowMillis: Long): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
}

@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var instance: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TaskDatabase::class.java,
                "taskwake.db",
            ).fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
