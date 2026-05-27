package com.odysee.app.feature.channel.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun ImageCropDialog(
    uri: Uri,
    aspectRatio: Float,
    circular: Boolean,
    onCancel: () -> Unit,
    onConfirm: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var frameSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    LaunchedEffect(uri) {
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                    BitmapFactory.decodeStream(input, null, opts)
                }
            }.getOrNull()
        }
        if (bmp == null) loadError = "Couldn't load image" else sourceBitmap = bmp
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel, enabled = !processing) {
                        Text("Cancel", color = Color.White)
                    }
                    Text(
                        text = "Adjust crop",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(
                        onClick = {
                            val bmp = sourceBitmap ?: return@TextButton
                            if (frameSize.width == 0 || frameSize.height == 0) return@TextButton
                            processing = true
                            val bytes = cropToBytes(
                                src = bmp,
                                scale = scale,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                frameW = frameSize.width,
                                frameH = frameSize.height,
                            )
                            onConfirm(bytes)
                        },
                        enabled = sourceBitmap != null && !processing,
                    ) {
                        Text("Done", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(if (circular) androidx.compose.foundation.shape.CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                        .background(Color(0xFF101010))
                        .onSizeChanged { frameSize = it }
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(0.5f, 6f)
                                val zoomRatio = newScale / scale
                                scale = newScale
                                offsetX = offsetX * zoomRatio + pan.x
                                offsetY = offsetY * zoomRatio + pan.y
                                val bmp = sourceBitmap
                                if (bmp != null && frameSize.width > 0 && frameSize.height > 0) {
                                    val fitted = fittedScale(bmp.width, bmp.height, frameSize.width, frameSize.height)
                                    val drawW = bmp.width * fitted * scale
                                    val drawH = bmp.height * fitted * scale
                                    val maxX = max(0f, (drawW - frameSize.width) / 2f)
                                    val maxY = max(0f, (drawH - frameSize.height) / 2f)
                                    offsetX = offsetX.coerceIn(-maxX, maxX)
                                    offsetY = offsetY.coerceIn(-maxY, maxY)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = sourceBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                ),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )
                    } else if (loadError != null) {
                        Text(loadError!!, color = Color.White)
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Pinch to zoom, drag to position",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun fittedScale(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Float {
    val sw = dstW.toFloat() / srcW
    val sh = dstH.toFloat() / srcH
    return max(sw, sh)
}

private fun cropToBytes(
    src: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    frameW: Int,
    frameH: Int,
): ByteArray {
    val fitted = fittedScale(src.width, src.height, frameW, frameH)
    val effective = fitted * scale
    val drawW = src.width * effective
    val drawH = src.height * effective
    val leftInFrame = (frameW - drawW) / 2f + offsetX
    val topInFrame = (frameH - drawH) / 2f + offsetY
    val cropLeftPx = ((-leftInFrame) / effective).coerceAtLeast(0f)
    val cropTopPx = ((-topInFrame) / effective).coerceAtLeast(0f)
    val cropWPx = min(src.width.toFloat() - cropLeftPx, frameW / effective)
    val cropHPx = min(src.height.toFloat() - cropTopPx, frameH / effective)
    val cropW = cropWPx.toInt().coerceAtLeast(1)
    val cropH = cropHPx.toInt().coerceAtLeast(1)
    val cropX = cropLeftPx.toInt().coerceAtLeast(0).coerceAtMost(src.width - cropW)
    val cropY = cropTopPx.toInt().coerceAtLeast(0).coerceAtMost(src.height - cropH)
    val cropped = Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)
    val out = ByteArrayOutputStream()
    cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
    if (cropped !== src) cropped.recycle()
    return out.toByteArray()
}
