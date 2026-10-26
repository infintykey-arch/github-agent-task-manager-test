package com.infintykey.taskwake

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TaskWakeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createReminderChannel()
    }

    fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.reminder_channel_description)
                lockscreenVisibility = NotificationManager.VISIBILITY_PRIVATE
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
