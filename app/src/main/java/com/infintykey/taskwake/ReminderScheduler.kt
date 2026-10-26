package com.infintykey.taskwake

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun schedule(context: Context, task: TaskEntity) {
        val atMillis = task.reminderAtMillis ?: return
        if (task.id <= 0L || !TaskTimeRules.isFutureReminder(atMillis, System.currentTimeMillis())) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            atMillis,
            pendingIntent(context, task, PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    fun cancel(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val pendingIntent = pendingIntent(context, TaskEntity(id = taskId, title = ""), PendingIntent.FLAG_NO_CREATE) ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntent(context: Context, task: TaskEntity, baseFlags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("${ReminderReceiver.ACTION_REMINDER_PREFIX}${task.id}")
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            .putExtra(ReminderReceiver.EXTRA_TITLE, task.title)
            .putExtra(ReminderReceiver.EXTRA_DESCRIPTION, task.description)
        val flags = baseFlags or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else 0
        return PendingIntent.getBroadcast(context, task.id.hashCode(), intent, flags)
    }
}
