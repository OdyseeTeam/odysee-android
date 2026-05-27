package com.odysee.app.player

import androidx.compose.runtime.staticCompositionLocalOf
import com.odysee.app.core.data.player.PlayerController

val LocalPlayerController = staticCompositionLocalOf<PlayerController> {
    error("PlayerController not provided. Wrap your composition in CompositionLocalProvider.")
}
