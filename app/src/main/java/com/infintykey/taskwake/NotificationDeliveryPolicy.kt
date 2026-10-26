package com.infintykey.taskwake

object NotificationDeliveryPolicy {
    fun canDeliver(runtimePermissionGranted: Boolean, appNotificationsEnabled: Boolean): Boolean =
        runtimePermissionGranted && appNotificationsEnabled
}
