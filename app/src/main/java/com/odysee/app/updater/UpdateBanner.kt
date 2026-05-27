package com.odysee.app.updater

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.data.updater.AppUpdater
import com.odysee.app.core.data.updater.UpdateState
import kotlinx.coroutines.launch

@Composable
fun UpdateBannerHost(appUpdater: AppUpdater, modifier: Modifier = Modifier) {
    if (!appUpdater.isSupported) return
    val state by appUpdater.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    when (val s = state) {
        is UpdateState.Available -> {
            Surface(
                modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                    ) {
                        Text(
                            text = "Update available",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Version ${s.info.displayVersion}",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(
                        onClick = { scope.launch { appUpdater.downloadAndInstall() } },
                    ) {
                        Text("Download", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { appUpdater.dismiss() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = Color.White)
                    }
                }
            }
        }
        is UpdateState.Downloading -> {
            Surface(
                modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "Downloading update… ${(s.progress * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = { s.progress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        }
        else -> Unit
    }
}
