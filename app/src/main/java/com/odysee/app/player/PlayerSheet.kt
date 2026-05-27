package com.odysee.app.player

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.data.player.PlayerOpenMode
import com.odysee.app.core.data.player.PlayerUiCommand
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val MINI_BAR_HEIGHT = 64.dp

@Composable
fun PlayerSheet(
    onChannelClick: (String, String) -> Unit,
    appContent: @Composable () -> Unit,
) {
    val controller = LocalPlayerController.current
    val state by controller.state.collectAsStateWithLifecycle()
    val isPipActive by controller.isPipActive.collectAsStateWithLifecycle()
    val media = state.media
    val hasMedia = media != null
    val context = LocalContext.current

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        controller.uiCommands.collect { cmd ->
            when (cmd) {
                is PlayerUiCommand.Show -> when (cmd.mode) {
                    PlayerOpenMode.Expanded -> {
                        FloatingPlayerService.stop(context)
                        isExpanded = true
                    }
                    PlayerOpenMode.Minimized -> {
                        FloatingPlayerService.stop(context)
                        isExpanded = false
                    }
                    PlayerOpenMode.Pip -> {
                        if (Settings.canDrawOverlays(context)) {
                            isExpanded = false
                            FloatingPlayerService.start(context)
                        } else {
                            Toast.makeText(
                                context,
                                "Grant \"Display over other apps\" permission to use pop-up player",
                                Toast.LENGTH_LONG,
                            ).show()
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            runCatching { context.startActivity(intent) }
                        }
                    }
                }
            }
        }
    }

    val density = LocalDensity.current
    val navBarsBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxHpx = constraints.maxHeight.toFloat()
        val miniHpx = with(density) { MINI_BAR_HEIGHT.toPx() }
        val expandedY = 0f
        val miniY = (maxHpx - miniHpx - navBarsBottomPx).coerceAtLeast(0f)
        val hiddenY = maxHpx

        // Keep the in-app player visible while PiP is active so the media page
        // (channel info, description, comments, related items) stays put.
        // ExpandedPlayerContent swaps the PlayerView for a thumbnail when
        // isPipActive is true.
        val showInAppPlayer = hasMedia

        val initialY = when {
            !showInAppPlayer -> hiddenY
            isExpanded -> expandedY
            else -> miniY
        }
        val offsetY = remember { Animatable(initialY) }

        LaunchedEffect(isExpanded, showInAppPlayer, miniY) {
            val target = when {
                !showInAppPlayer -> hiddenY
                isExpanded -> expandedY
                else -> miniY
            }
            offsetY.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }

        val expandFraction = if (miniY <= 0f) 1f else
            ((miniY - offsetY.value) / miniY).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showInAppPlayer) MINI_BAR_HEIGHT else 0.dp),
        ) {
            appContent()
        }

        if (showInAppPlayer) {
            val draggableState = rememberDraggableState { delta ->
                scope.launch {
                    offsetY.snapTo((offsetY.value + delta).coerceIn(expandedY, miniY))
                }
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val collapseByVel = velocity > 800f
                            val expandByVel = velocity < -800f
                            val byPosition = offsetY.value < miniY / 2f
                            isExpanded = when {
                                expandByVel -> true
                                collapseByVel -> false
                                else -> byPosition
                            }
                        },
                    ),
            ) {
                if (expandFraction > 0.02f) {
                    Box(modifier = Modifier.fillMaxSize().alpha(expandFraction)) {
                        ExpandedPlayerContent(
                            state = state,
                            controller = controller,
                            onCollapse = { isExpanded = false },
                            onChannelClick = onChannelClick,
                        )
                    }
                }
                if (expandFraction < 0.98f) {
                    Box(
                        modifier = Modifier
                            .height(MINI_BAR_HEIGHT)
                            .fillMaxWidth()
                            .alpha(1f - expandFraction),
                    ) {
                        MiniPlayerBar(
                            media = media!!,
                            isPlaying = state.isPlaying,
                            isResolving = state.isResolving,
                            onTap = { isExpanded = true },
                            onPlayPause = controller::togglePlayPause,
                            onClose = controller::close,
                        )
                    }
                }
            }
        }
    }
}
