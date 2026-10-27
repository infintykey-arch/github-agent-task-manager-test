package com.infintykey.taskwake

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val url: String = "",
    val durationMinutes: Int? = null,
    val reminderAtMillis: Long? = null,
    val isCompleted: Boolean = false,
)

object TaskTimeRules {
    fun isFutureReminder(reminderAtMillis: Long?, nowMillis: Long): Boolean =
        reminderAtMillis != null && reminderAtMillis > nowMillis
}
