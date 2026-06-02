package com.odysee.app.core.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowSize {
    Compact,
    Medium,
    Expanded,
    Wide,
}

@Composable
@ReadOnlyComposable
fun rememberWindowSize(): WindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> WindowSize.Compact
        widthDp < 840 -> WindowSize.Medium
        widthDp < 1200 -> WindowSize.Expanded
        else -> WindowSize.Wide
    }
}

fun WindowSize.feedColumns(): Int = when (this) {
    WindowSize.Compact -> 1
    WindowSize.Medium -> 2
    WindowSize.Expanded -> 3
    WindowSize.Wide -> 4
}
