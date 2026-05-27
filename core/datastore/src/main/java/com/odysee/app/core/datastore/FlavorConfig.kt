package com.odysee.app.core.datastore

interface FlavorConfig {
    /** Whether the running build can use Firebase / FCM push notifications. */
    val pushSupported: Boolean

    /** Default delivery mode for this flavor before the user changes it. */
    val defaultDeliveryMode: NotificationDeliveryMode
}
