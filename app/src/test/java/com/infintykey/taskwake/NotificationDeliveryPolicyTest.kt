package com.infintykey.taskwake

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeliveryPolicyTest {
    @Test
    fun notificationIsAllowedOnlyWhenPermissionAndAppNotificationsAreEnabled() {
        assertTrue(NotificationDeliveryPolicy.canDeliver(true, true))
        assertFalse(NotificationDeliveryPolicy.canDeliver(false, true))
        assertFalse(NotificationDeliveryPolicy.canDeliver(true, false))
        assertFalse(NotificationDeliveryPolicy.canDeliver(false, false))
    }
}
