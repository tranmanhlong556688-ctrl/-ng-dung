package vn.mltudonghoa.cuncung

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.abs
import kotlin.random.Random

class DogOverlayService : Service() {
    companion object {
        const val ACTION_START = "vn.mltudonghoa.cuncung.START"
        const val ACTION_STOP = "vn.mltudonghoa.cuncung.STOP"
        const val ACTION_SPEAK = "vn.mltudonghoa.cuncung.SPEAK"
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
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var downAt = 0L

    private val mover = object : Runnable {
        override fun run() {
            val view = dogView ?: return
            val lp = params ?: return
            if (Prefs.autoMove(this@DogOverlayService)) {
                view.setMood(DogView.Mood.RUNNING)
                val metrics = resources.displayMetrics
                val maxX = (metrics.widthPixels - view.width.coerceAtLeast(250)).coerceAtLeast(0)
                val maxY = (metrics.heightPixels - view.height.coerceAtLeast(250) - 100).coerceAtLeast(0)
                val targetX = Random.nextInt(0, maxX + 1)
                val targetY = Random.nextInt(80, maxY.coerceAtLeast(80) + 1)
                animateTo(targetX, targetY)
            }
            handler.postDelayed(this, 8500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
            else -> ensureOverlay()
        }
        return START_STICKY
    }

    private fun ensureOverlay() {
        if (dogView != null) return
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Hãy cấp quyền hiển thị trên ứng dụng khác.", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        val view = DogView(this).apply {
            contentDescription = "Chú cún nổi ML"
            setOverlayTouchHandler(::handleTouch)
        }
        val lp = WindowManager.LayoutParams(
            dp(170), dp(190), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(20)
            y = dp(160)
        }
        windowManager.addView(view, lp)
        dogView = view
        params = lp
        handler.postDelayed(mover, 2500)
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
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                lp.x = startX + (event.rawX - downX).toInt()
                lp.y = startY + (event.rawY - downY).toInt()
                dogView?.let { windowManager.updateViewLayout(it, lp) }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val moved = abs(event.rawX - downX) + abs(event.rawY - downY)
                val held = System.currentTimeMillis() - downAt
                if (moved < 20f) {
                    if (held > 650) {
                        dogView?.speak("Chạm thông báo để mở bảng điều khiển nhé!")
                    } else {
                        val phrases = listOf(
                            "Gâu! Chúc bạn làm việc vui vẻ!",
                            "Nhớ uống nước nhé!",
                            "Cố lên, bạn đang làm rất tốt!",
                            "Mình ở đây cùng bạn!"
                        )
                        dogView?.speak(phrases.random())
                    }
                }
                return true
            }
        }
        return true
    }

    private fun animateTo(targetX: Int, targetY: Int) {
        val lp = params ?: return
        val view = dogView ?: return
        val sx = lp.x
        val sy = lp.y
        val steps = 60
        var step = 0
        val runner = object : Runnable {
            override fun run() {
                if (dogView == null) return
                step++
                val t = step / steps.toFloat()
                lp.x = (sx + (targetX - sx) * t).toInt()
                lp.y = (sy + (targetY - sy) * t).toInt()
                windowManager.updateViewLayout(view, lp)
                if (step < steps) handler.postDelayed(this, 16) else view.setMood(DogView.Mood.HAPPY)
            }
        }
        handler.post(runner)
    }

    private fun playAudio(value: String) {
        try {
            mediaPlayer?.release()
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            if (value.startsWith("content://")) player.setDataSource(this, Uri.parse(value)) else player.setDataSource(value)
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
            channel.description = "Giữ chú cún hoạt động trên màn hình"
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
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("ML Cún Cưng đang chạy")
            .setContentText("Chạm để mở cài đặt")
            .setContentIntent(openIntent)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Tắt cún", stopIntent).build())
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        dogView?.let { runCatching { windowManager.removeView(it) } }
        dogView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
