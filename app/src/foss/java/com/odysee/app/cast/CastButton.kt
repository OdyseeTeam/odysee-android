package com.odysee.app.cast

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * FOSS variant has no Cast SDK — the button renders nothing.
 */
@Composable
fun OdyseeCastButton(modifier: Modifier = Modifier) {
    // intentionally empty
}

fun isCastAvailable(context: Context): Boolean = false
