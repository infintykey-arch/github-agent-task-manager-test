package com.infintykey.taskwake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = TaskDatabase.getInstance(context).taskDao()
                    .getFutureReminders(System.currentTimeMillis())
                tasks.forEach { ReminderScheduler.schedule(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
