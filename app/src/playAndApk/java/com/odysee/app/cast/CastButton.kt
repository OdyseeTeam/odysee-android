package com.odysee.app.cast

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext

/**
 * Renders the standard Cast button. MediaRouteButton's internal theme helper
 * crashes ("background can not be translucent") if it's constructed against a
 * Compose activity context whose theme has a transparent window background,
 * so we wrap the context in an opaque AppCompat theme before instantiating.
 */
@Composable
fun OdyseeCastButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        runCatching { CastContext.getSharedInstance(context) }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val themed = ContextThemeWrapper(ctx, com.odysee.app.R.style.Theme_Odysee_MediaRouter)
            MediaRouteButton(themed)
        },
        update = { button ->
            runCatching { CastButtonFactory.setUpMediaRouteButton(context, button) }
        },
    )
}

fun isCastAvailable(context: Context): Boolean =
    runCatching { CastContext.getSharedInstance(context); true }.getOrDefault(false)
