package com.infintykey.taskwake

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun futureReminderCreatesAndCancellationRemovesItsPendingIntent() {
        val taskId = System.currentTimeMillis()
        val task = TaskEntity(
            id = taskId,
            title = "اختبار تذكير",
            description = "يجب ألا يصل هذا الاختبار إلى إشعار المستخدم",
            reminderAtMillis = System.currentTimeMillis() + 60_000L,
        )
        ReminderScheduler.schedule(context, task)

        val action = "${ReminderReceiver.ACTION_REMINDER_PREFIX}$taskId"
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        assertNotNull("The alarm PendingIntent should exist after scheduling", pendingIntent)

        ReminderScheduler.cancel(context, taskId)

        val afterCancel = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        assertNull("The alarm PendingIntent should be removed after cancellation", afterCancel)
    }
}
