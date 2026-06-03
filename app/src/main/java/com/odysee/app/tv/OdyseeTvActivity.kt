package com.odysee.app.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.odysee.app.core.data.player.PlayerController
import com.odysee.app.core.designsystem.theme.OdyseeTheme
import com.odysee.app.player.LocalPlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OdyseeTvActivity : ComponentActivity() {
    @Inject lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            OdyseeTheme {
                CompositionLocalProvider(LocalPlayerController provides playerController) {
                    OdyseeTvApp()
                }
            }
        }
    }
}
