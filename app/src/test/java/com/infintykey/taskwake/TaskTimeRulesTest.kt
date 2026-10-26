package com.infintykey.taskwake

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTimeRulesTest {
    @Test
    fun futureReminderIsAccepted() {
        assertTrue(TaskTimeRules.isFutureReminder(1_001L, 1_000L))
    }

    @Test
    fun reminderAtNowOrInPastIsRejected() {
        assertFalse(TaskTimeRules.isFutureReminder(1_000L, 1_000L))
        assertFalse(TaskTimeRules.isFutureReminder(999L, 1_000L))
    }

    @Test
    fun absentReminderIsNotScheduled() {
        assertFalse(TaskTimeRules.isFutureReminder(null, 1_000L))
    }
}
