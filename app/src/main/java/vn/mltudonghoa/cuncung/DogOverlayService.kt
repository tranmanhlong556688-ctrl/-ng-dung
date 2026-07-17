package vn.mltudonghoa.cuncung

import android.animation.ValueAnimator
import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class DogOverlayService : Service() {
    companion object {
        const val ACTION_START = "vn.mltudonghoa.cuncung.START"
        const val ACTION_STOP = "vn.mltudonghoa.cuncung.STOP"
        const val ACTION_SPEAK = "vn.mltudonghoa.cuncung.SPEAK"
        const val ACTION_REFRESH = "vn.mltudonghoa.cuncung.REFRESH"
        const val ACTION_TWIN_NOW = "vn.mltudonghoa.cuncung.TWIN_NOW"
        const val EXTRA_TEXT = "text"
        const val EXTRA_AUDIO = "audio"
        private const val CHANNEL_ID = "dog_overlay"
        private const val NOTIFICATION_ID = 1607
    }

    private lateinit var windowManager: WindowManager
    private var dogView: DogView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var twinAnimator: ValueAnimator? = null
    private var isTwinRunning = false
    private var screenActive = true
    private var lowBattery = false
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var downAt = 0L

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenActive = false
                    dogView?.setScreenActive(false)
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenActive = true
                    dogView?.setScreenActive(true)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    val charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                    lowBattery = Prefs.pauseLowBattery(this@DogOverlayService) && !charging && level * 100 / scale <= 15
                    dogView?.setLowBattery(lowBattery)
                }
            }
        }
    }

    private val mover = object : Runnable {
        override fun run() {
            val view = dogView ?: return
            if (screenActive && !lowBattery && !isTwinRunning && Prefs.autoMove(this@DogOverlayService)) {
                val target = chooseTarget(view)
                animateTo(target.first, target.second)
            } else if (lowBattery) {
                view.setMoving(false)
                view.setMood(DogView.Mood.SLEEPING)
            }
            handler.postDelayed(this, if (lowBattery) 20_000L else Random.nextLong(6_500L, 10_500L))
        }
    }

    private val twinScheduler = object : Runnable {
        override fun run() {
            if (screenActive && !lowBattery && Prefs.twinEnabled(this@DogOverlayService) && !isTwinRunning) {
                startTwinEffect()
            }
            val minutes = Prefs.twinIntervalMinutes(this@DogOverlayService).toLong()
            handler.postDelayed(this, minutes * 60_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(stateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SPEAK -> {
                ensureOverlay()
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty().ifBlank { "Đến giờ rồi!" }
                dogView?.speak(text)
                intent.getStringExtra(EXTRA_AUDIO)?.let(::playAudio)
            }
            ACTION_REFRESH -> applyPreferences()
            ACTION_TWIN_NOW -> {
                ensureOverlay()
                startTwinEffect()
            }
            else -> ensureOverlay()
        }
        return START_STICKY
    }

    private fun ensureOverlay() {
        if (dogView != null) {
            applyPreferences()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Hãy cấp quyền hiển thị trên ứng dụng khác.", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        val view = DogView(this).apply {
            contentDescription = "Chú cún bán 3D nổi ML"
            setOverlayTouchHandler(::handleTouch)
            setQuality(Prefs.quality(this@DogOverlayService))
            setLowBattery(lowBattery)
            setScreenActive(screenActive)
        }
        val lp = WindowManager.LayoutParams(
            dp(310), dp(245), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(8)
            y = dp(150)
        }
        windowManager.addView(view, lp)
        dogView = view
        params = lp
        updateDepth()
        handler.postDelayed(mover, 2200L)
        handler.postDelayed(twinScheduler, 45_000L)
    }

    private fun applyPreferences() {
        dogView?.setQuality(Prefs.quality(this))
        dogView?.setLowBattery(lowBattery)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val lp = params ?: return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = lp.x
                startY = lp.y
                downAt = System.currentTimeMillis()
                dogView?.setMood(DogView.Mood.HAPPY)
                dogView?.setMoving(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val screenW = resources.displayMetrics.widthPixels
                val screenH = resources.displayMetrics.heightPixels
                lp.x = (startX + (event.rawX - downX).toInt()).coerceIn(-dp(70), screenW - dp(240))
                lp.y = (startY + (event.rawY - downY).toInt()).coerceIn(dp(20), screenH - dp(210))
                dogView?.let { windowManager.updateViewLayout(it, lp) }
                updateDepth()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dogView?.setMoving(false)
                val moved = abs(event.rawX - downX) + abs(event.rawY - downY)
                val held = System.currentTimeMillis() - downAt
                if (moved < 24f) {
                    if (held > 650) {
                        dogView?.speak("Mở ứng dụng để chỉnh cún và vùng icon nhé!")
                    } else {
                        dogView?.speak(listOf(
                            "Gâu! Hôm nay mình cùng cố gắng nhé!",
                            "Nhớ uống nước và nghỉ mắt nhé!",
                            "Mình đang canh các icon giúp bạn!",
                            "Chạm lần nữa, mình sẽ vui hơn!"
                        ).random())
                    }
                }
                return true
            }
        }
        return true
    }

    private fun chooseTarget(view: DogView): Pair<Int, Int> {
        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - view.width.coerceAtLeast(dp(260))).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - view.height.coerceAtLeast(dp(220)) - dp(50)).coerceAtLeast(dp(80))
        val markers = Prefs.markers(this)
        if (markers.isNotEmpty() && Random.nextFloat() < 0.78f) {
            val point = markers.random()
            val angle = Random.nextDouble(0.0, Math.PI * 2.0)
            val radius = dp(Random.nextInt(45, 86))
            val iconX = (point.x * metrics.widthPixels).toInt()
            val iconY = (point.y * metrics.heightPixels).toInt()
            val x = (iconX + cos(angle) * radius - view.width / 2).toInt().coerceIn(0, maxX)
            val y = (iconY + sin(angle) * radius - view.height / 2).toInt().coerceIn(dp(50), maxY)
            return x to y
        }
        return Random.nextInt(0, maxX + 1) to Random.nextInt(dp(70), maxY + 1)
    }

    private fun animateTo(targetX: Int, targetY: Int) {
        val lp = params ?: return
        val view = dogView ?: return
        val sx = lp.x
        val sy = lp.y
        val dx = targetX - sx
        val dy = targetY - sy
        view.setDirection(directionFrom(dx, dy))
        view.setMoving(true)
        val duration = when (Prefs.quality(this)) {
            Prefs.QUALITY_SAVER -> 3400L
            Prefs.QUALITY_BEST -> 2300L
            else -> 2750L
        }
        val started = SystemClock.uptimeMillis()
        val runner = object : Runnable {
            override fun run() {
                if (dogView == null || !screenActive || isTwinRunning) {
                    view.setMoving(false)
                    return
                }
                val t = ((SystemClock.uptimeMillis() - started) / duration.toFloat()).coerceIn(0f, 1f)
                val smooth = t * t * (3f - 2f * t)
                lp.x = (sx + dx * smooth).toInt()
                lp.y = (sy + dy * smooth).toInt()
                runCatching { windowManager.updateViewLayout(view, lp) }
                updateDepth()
                if (t < 1f) handler.postDelayed(this, activeFrameDelay())
                else {
                    view.setMoving(false)
                    view.setMood(if (Random.nextFloat() < 0.22f) DogView.Mood.SNIFFING else DogView.Mood.HAPPY)
                }
            }
        }
        handler.post(runner)
    }

    private fun startTwinEffect() {
        val view = dogView ?: return
        if (isTwinRunning || !Prefs.twinEnabled(this)) return
        isTwinRunning = true
        view.setMood(DogView.Mood.PLAYING)
        view.setMoving(true)
        twinAnimator?.cancel()
        twinAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3500L
            addUpdateListener { view.setTwinProgress(it.animatedValue as Float) }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    finishTwinEffect()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    view.setTwinProgress(null)
                    isTwinRunning = false
                }
            })
            start()
        }
    }

    private fun finishTwinEffect() {
        val view = dogView ?: return
        view.setTwinProgress(null)
        view.setMoving(false)
        view.setMood(DogView.Mood.HAPPY)
        isTwinRunning = false
        view.speak("Hai đứa nhập lại thành một rồi! Gâu!")
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (!Prefs.isQuietNow(this, hour)) {
            when (Prefs.mergeAudio(this)) {
                Prefs.MERGE_AUDIO_RECORDING -> Prefs.recordingPath(this)?.let(::playAudio)
                Prefs.MERGE_AUDIO_SELECTED -> Prefs.audioUri(this)?.let(::playAudio)
            }
        }
    }

    private fun directionFrom(dx: Int, dy: Int): DogView.Direction {
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        return when {
            angle >= -22.5 && angle < 22.5 -> DogView.Direction.RIGHT
            angle >= 22.5 && angle < 67.5 -> DogView.Direction.DOWN_RIGHT
            angle >= 67.5 && angle < 112.5 -> DogView.Direction.DOWN
            angle >= 112.5 && angle < 157.5 -> DogView.Direction.DOWN_LEFT
            angle >= 157.5 || angle < -157.5 -> DogView.Direction.LEFT
            angle >= -157.5 && angle < -112.5 -> DogView.Direction.UP_LEFT
            angle >= -112.5 && angle < -67.5 -> DogView.Direction.UP
            else -> DogView.Direction.UP_RIGHT
        }
    }

    private fun updateDepth() {
        val lp = params ?: return
        val max = (resources.displayMetrics.heightPixels - dp(220)).coerceAtLeast(1)
        val normalized = (lp.y.toFloat() / max).coerceIn(0f, 1f)
        dogView?.setDepthScale(0.76f + normalized * 0.38f)
    }

    private fun activeFrameDelay(): Long = when (Prefs.quality(this)) {
        Prefs.QUALITY_SAVER -> 66L
        else -> 33L
    }

    private fun playAudio(value: String) {
        if (value.isBlank()) return
        try {
            mediaPlayer?.release()
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            if (value.startsWith("content://")) player.setDataSource(this, Uri.parse(value))
            else player.setDataSource(value)
            player.setOnCompletionListener { it.release(); mediaPlayer = null }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            Toast.makeText(this, "Không phát được âm thanh: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "ML Cún Cưng", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Giữ chú cún bán 3D hoạt động trên màn hình"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, DogOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val twinIntent = PendingIntent.getService(
            this, 3, Intent(this, DogOverlayService::class.java).setAction(ACTION_TWIN_NOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("ML Cún Cưng Semi 3D đang chạy")
            .setContentText("Chạm để mở cài đặt")
            .setContentIntent(openIntent)
            .addAction(Notification.Action.Builder(null, "Nhân đôi", twinIntent).build())
            .addAction(Notification.Action.Builder(null, "Tắt cún", stopIntent).build())
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        twinAnimator?.cancel()
        mediaPlayer?.release()
        dogView?.release()
        dogView?.let { runCatching { windowManager.removeView(it) } }
        dogView = null
        runCatching { unregisterReceiver(stateReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
