package com.odysee.app.flavor

import com.odysee.app.core.datastore.FlavorConfig
import com.odysee.app.core.datastore.NotificationDeliveryMode
import javax.inject.Inject

class FlavorConfigImpl @Inject constructor() : FlavorConfig {
    override val pushSupported: Boolean = false
    override val defaultDeliveryMode: NotificationDeliveryMode = NotificationDeliveryMode.Poll
}
