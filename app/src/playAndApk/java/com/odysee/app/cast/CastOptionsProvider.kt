package com.odysee.app.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.odysee.app.R

/**
 * Loaded by the Cast SDK from the manifest meta-data. We use the default
 * media receiver (so any Chromecast on the network is reachable) but brand
 * the local Android cast notification with Odysee's icon + pink accent so it
 * matches the rest of the app.
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName("com.odysee.app.MainActivity")
            .setSmallIconDrawableResId(R.drawable.ic_stat_odysee)
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(
                com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID,
            )
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
