package com.odysee.app.player

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.odysee.app.MainActivity
import com.odysee.app.R
import com.odysee.app.core.data.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@AndroidEntryPoint
class FloatingPlayerService : Service() {

    @Inject lateinit var playerController: PlayerController

    private var windowManager: WindowManager? = null
    private var rootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var controlsOverlay: View? = null
    private var playPauseButton: ImageButton? = null
    private var progressBar: SeekBar? = null
    private var dropTargetView: ImageButton? = null
    private var dropTargetParams: WindowManager.LayoutParams? = null
    private val dropTargetBaseSize get() = dp(56f)
    private val dropTargetHotSize get() = dp(72f)
    private val dropTargetHitRadius get() = dp(80f)
    private var playerListener: Player.Listener? = null
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { setControlsVisible(false) }
    private val progressHandler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private val progressRunnable = object : Runnable {
        override fun run() {
            refreshProgress()
            progressHandler.postDelayed(this, 500L)
        }
    }

    private var minWidthPx = 0
    private var maxWidthPx = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
        playerController.setPipActive(true)
    }

    private fun dp(value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics,
    ).roundToInt()

    private fun showOverlay() {
        val container = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }

        val playerView = (LayoutInflater.from(this).inflate(R.layout.odysee_player_view, container, false) as PlayerView).apply {
            useController = false
            player = playerController.exoPlayer
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        container.addView(playerView)

        val controls = buildControls(container)
        container.addView(controls)
        controlsOverlay = controls
        controls.visibility = View.GONE

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val screenWidth = resources.displayMetrics.widthPixels
        minWidthPx = dp(160f)
        maxWidthPx = screenWidth.coerceAtLeast(minWidthPx)

        val initialWidth = dp(240f).coerceIn(minWidthPx, maxWidthPx)
        val initialHeight = (initialWidth * 9) / 16
        val params = WindowManager.LayoutParams(
            initialWidth, initialHeight, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - initialWidth - dp(12f)
            y = resources.displayMetrics.heightPixels - initialHeight - dp(160f)
        }

        attachGestures(container, params)
        attachPlayerListener()

        windowManager?.addView(container, params)
        rootView = container
        layoutParams = params

        refreshPlayPauseIcon()
    }

    private fun buildControls(parent: ViewGroup): View {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x66000000)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val expandButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setBackgroundColor(0x00000000)
            setColorFilter(0xFFFFFFFF.toInt())
            layoutParams = FrameLayout.LayoutParams(
                dp(32f), dp(32f), Gravity.TOP or Gravity.START,
            ).apply { setMargins(dp(4f), dp(4f), 0, 0) }
            setOnClickListener {
                playerController.requestExpand()
                val launch = Intent(this@FloatingPlayerService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(launch)
                stopSelf()
            }
        }
        overlay.addView(expandButton)

        val closeButton = ImageButton(this).apply {
            setImageResource(com.odysee.app.R.drawable.ic_pip_close)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFFE2202D.toInt())
            }
            layoutParams = FrameLayout.LayoutParams(
                dp(32f), dp(32f), Gravity.TOP or Gravity.END,
            ).apply { setMargins(0, dp(4f), dp(4f), 0) }
            setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            alpha = 1f
            setOnClickListener { stopSelf() }
        }
        overlay.addView(closeButton)

        val play = ImageButton(this).apply {
            setBackgroundColor(0x00000000)
            setColorFilter(0xFFFFFFFF.toInt())
            layoutParams = FrameLayout.LayoutParams(
                dp(48f), dp(48f), Gravity.CENTER,
            )
            setOnClickListener {
                playerController.togglePlayPause()
                refreshPlayPauseIcon()
                scheduleAutoHide()
            }
        }
        overlay.addView(play)
        playPauseButton = play

        val seek = SeekBar(this).apply {
            max = 1000
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ).apply { setMargins(dp(8f), 0, dp(8f), dp(2f)) }
            progressTintList = android.content.res.ColorStateList.valueOf(0xFFE2202D.toInt())
            thumbTintList = android.content.res.ColorStateList.valueOf(0xFFE2202D.toInt())
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0x66FFFFFF)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(bar: SeekBar) {
                    isUserSeeking = true
                    cancelAutoHide()
                }
                override fun onStopTrackingTouch(bar: SeekBar) {
                    val duration = playerController.exoPlayer.duration
                    if (duration > 0) {
                        val target = (bar.progress.toLong() * duration) / 1000L
                        playerController.exoPlayer.seekTo(target)
                    }
                    isUserSeeking = false
                    scheduleAutoHide()
                }
            })
        }
        overlay.addView(seek)
        progressBar = seek

        return overlay
    }

    private fun attachGestures(view: View, params: WindowManager.LayoutParams) {
        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val newW = (params.width * detector.scaleFactor).toInt().coerceIn(minWidthPx, maxWidthPx)
                val newH = (newW * 9) / 16
                if (newW != params.width) {
                    val centerX = params.x + params.width / 2
                    val centerY = params.y + params.height / 2
                    params.width = newW
                    params.height = newH
                    val maxX = resources.displayMetrics.widthPixels - newW
                    val maxY = resources.displayMetrics.heightPixels - newH
                    params.x = (centerX - newW / 2).coerceIn(0, maxX.coerceAtLeast(0))
                    params.y = (centerY - newH / 2).coerceIn(0, maxY.coerceAtLeast(0))
                    runCatching { windowManager?.updateViewLayout(view, params) }
                }
                return true
            }
        })

        view.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f
            private var moved = false
            private val slop = dp(8f).toFloat()
            private var scaling = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (event.pointerCount > 1) {
                    scaling = true
                    scaleDetector.onTouchEvent(event)
                    return true
                }
                if (scaling) {
                    if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        scaling = false
                    }
                    return true
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                        moved = false
                        cancelAutoHide()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - touchStartX
                        val dy = event.rawY - touchStartY
                        if (!moved && (abs(dx) > slop || abs(dy) > slop)) {
                            moved = true
                            showDropTarget()
                        }
                        if (moved) {
                            val maxX = resources.displayMetrics.widthPixels - params.width
                            val maxY = resources.displayMetrics.heightPixels - params.height
                            params.x = (startX + dx).roundToInt().coerceIn(0, maxX.coerceAtLeast(0))
                            params.y = (startY + dy).roundToInt().coerceIn(0, maxY.coerceAtLeast(0))
                            runCatching { windowManager?.updateViewLayout(v, params) }
                            updateDropTargetHighlight(params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (moved) {
                            if (isOverDropTarget(params)) {
                                hideDropTarget()
                                stopSelf()
                                return true
                            }
                            scheduleAutoHide()
                        } else {
                            toggleControls()
                        }
                        hideDropTarget()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun dropTargetCenterX(): Int = resources.displayMetrics.widthPixels / 2

    private fun dropTargetCenterY(): Int =
        resources.displayMetrics.heightPixels - dp(96f)

    private fun overlayCenter(params: WindowManager.LayoutParams): Pair<Int, Int> {
        val cx = params.x + params.width / 2
        val cy = params.y + params.height / 2
        return cx to cy
    }

    private fun isOverDropTarget(params: WindowManager.LayoutParams): Boolean {
        val (cx, cy) = overlayCenter(params)
        val dx = (cx - dropTargetCenterX()).toFloat()
        val dy = (cy - dropTargetCenterY()).toFloat()
        return kotlin.math.hypot(dx, dy) <= dropTargetHitRadius.toFloat()
    }

    private fun showDropTarget() {
        if (dropTargetView != null) return
        val wm = windowManager ?: return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val size = dropTargetBaseSize
        val params = WindowManager.LayoutParams(
            size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dropTargetCenterX() - size / 2
            y = dropTargetCenterY() - size / 2
        }
        val button = ImageButton(this).apply {
            setImageResource(com.odysee.app.R.drawable.ic_pip_close)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFFE2202D.toInt())
            }
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            alpha = 1f
        }
        runCatching { wm.addView(button, params) }
        dropTargetView = button
        dropTargetParams = params
    }

    private fun updateDropTargetHighlight(overlayParams: WindowManager.LayoutParams) {
        val view = dropTargetView ?: return
        val params = dropTargetParams ?: return
        val wm = windowManager ?: return
        val hot = isOverDropTarget(overlayParams)
        val targetSize = if (hot) dropTargetHotSize else dropTargetBaseSize
        if (params.width != targetSize) {
            params.width = targetSize
            params.height = targetSize
            params.x = dropTargetCenterX() - targetSize / 2
            params.y = dropTargetCenterY() - targetSize / 2
            runCatching { wm.updateViewLayout(view, params) }
        }
        view.alpha = 1f
    }

    private fun hideDropTarget() {
        val view = dropTargetView ?: return
        runCatching { windowManager?.removeView(view) }
        dropTargetView = null
        dropTargetParams = null
    }

    private fun toggleControls() {
        val overlay = controlsOverlay ?: return
        if (overlay.visibility == View.VISIBLE) {
            setControlsVisible(false)
        } else {
            setControlsVisible(true)
            scheduleAutoHide()
        }
    }

    private fun setControlsVisible(visible: Boolean) {
        val overlay = controlsOverlay ?: return
        overlay.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) {
            cancelAutoHide()
            progressHandler.removeCallbacks(progressRunnable)
        } else {
            progressHandler.removeCallbacks(progressRunnable)
            progressHandler.post(progressRunnable)
        }
    }

    private fun refreshProgress() {
        val bar = progressBar ?: return
        if (isUserSeeking) return
        val duration = playerController.exoPlayer.duration
        val position = playerController.exoPlayer.currentPosition
        if (duration > 0) {
            bar.progress = ((position * 1000L) / duration).toInt().coerceIn(0, 1000)
        } else {
            bar.progress = 0
        }
    }

    private fun scheduleAutoHide() {
        cancelAutoHide()
        hideHandler.postDelayed(hideRunnable, 3000L)
    }

    private fun cancelAutoHide() {
        hideHandler.removeCallbacks(hideRunnable)
    }

    private fun attachPlayerListener() {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                refreshPlayPauseIcon()
            }
        }
        playerListener = listener
        playerController.exoPlayer.addListener(listener)
    }

    private fun refreshPlayPauseIcon() {
        val button = playPauseButton ?: return
        button.setImageResource(
            if (playerController.exoPlayer.isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play,
        )
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        clampToScreen()
    }

    private fun clampToScreen() {
        val params = layoutParams ?: return
        val view = rootView ?: return
        val wm = windowManager ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        minWidthPx = dp(160f)
        maxWidthPx = screenWidth.coerceAtLeast(minWidthPx)
        if (params.width > maxWidthPx) {
            params.width = maxWidthPx
            params.height = (params.width * 9) / 16
        }
        val maxX = (screenWidth - params.width).coerceAtLeast(0)
        val maxY = (screenHeight - params.height).coerceAtLeast(0)
        params.x = params.x.coerceIn(0, maxX)
        params.y = params.y.coerceIn(0, maxY)
        runCatching { wm.updateViewLayout(view, params) }
    }

    override fun onDestroy() {
        cancelAutoHide()
        progressHandler.removeCallbacks(progressRunnable)
        playerListener?.let { playerController.exoPlayer.removeListener(it) }
        playerListener = null
        hideDropTarget()
        rootView?.let { v ->
            runCatching { windowManager?.removeView(v) }
        }
        rootView = null
        layoutParams = null
        controlsOverlay = null
        playPauseButton = null
        progressBar = null
        playerController.setPipActive(false)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, FloatingPlayerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingPlayerService::class.java))
        }
    }
}
