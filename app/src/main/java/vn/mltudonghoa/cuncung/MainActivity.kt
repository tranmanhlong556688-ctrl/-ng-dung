package vn.mltudonghoa.cuncung

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    companion object {
        private const val REQUEST_AUDIO = 4101
        private const val REQUEST_MIC = 2001
        private const val REQUEST_NOTIFICATION = 2002
    }

    private lateinit var status: TextView
    private lateinit var messageInput: EditText
    private lateinit var delayInput: EditText
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentRecording: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNotificationPermission()
        updateStatus()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(245, 248, 255)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(40))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "🐶  ML Cún Cưng"
            textSize = 29f
            setTextColor(Color.rgb(7, 59, 140))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Thú cưng nổi • Nhắc việc • Ghi âm • Phát nhạc"
            textSize = 15f
            setTextColor(Color.rgb(55, 75, 110))
            setPadding(0, dp(4), 0, dp(14))
        })

        status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(16, 33, 61))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(status, matchWrap())

        root.addView(section("Chú cún trên màn hình"))
        root.addView(button("1. Cấp quyền hiển thị nổi") { openOverlayPermission() })
        root.addView(button("2. Bật chú cún") { startDog() })
        root.addView(button("Tắt chú cún") { stopDog() })

        root.addView(CheckBox(this).apply {
            text = "Cho cún tự chạy nhảy"
            isChecked = Prefs.autoMove(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> Prefs.setAutoMove(this@MainActivity, checked) }
        })
        root.addView(CheckBox(this).apply {
            text = "Tự bật lại sau khi khởi động điện thoại"
            isChecked = Prefs.autoStart(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> Prefs.setAutoStart(this@MainActivity, checked) }
        })

        root.addView(section("Nhắc việc"))
        messageInput = EditText(this).apply {
            hint = "Ví dụ: Đến giờ kiểm tra PCCC"
            setText("Đến giờ thực hiện công việc rồi!")
        }
        root.addView(messageInput, matchWrap())
        delayInput = EditText(this).apply {
            hint = "Số phút từ bây giờ"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }
        root.addView(delayInput, matchWrap())
        root.addView(button("Đặt lời nhắc") { scheduleReminder(null) })

        root.addView(section("Ghi và phát lại giọng nói"))
        root.addView(button("Bắt đầu ghi âm") { startRecording() })
        root.addView(button("Dừng và lưu ghi âm") { stopRecording() })
        root.addView(button("Nghe lại giọng vừa ghi") { playRecording() })
        root.addView(button("Hẹn cún phát giọng đã ghi") { scheduleReminder(Prefs.recordingPath(this)) })

        root.addView(section("Âm thanh và bài nhạc"))
        root.addView(button("Chọn MP3 / M4A / WAV") { pickAudio() })
        root.addView(button("Phát âm thanh đã chọn") { playSelectedAudio() })
        root.addView(button("Hẹn phát âm thanh đã chọn") { scheduleReminder(Prefs.audioUri(this)) })

        root.addView(section("Hướng dẫn Xiaomi Redmi 10"))
        root.addView(TextView(this).apply {
            text = "• Cấp quyền Cửa sổ nổi.\n• Cho phép Thông báo và Micro.\n• Pin → Không hạn chế cho ML Cún Cưng.\n• Bật Tự khởi chạy nếu muốn cún hoạt động sau khi mở máy.\n• Chạm cún để nghe lời động viên; kéo cún để đổi vị trí."
            textSize = 15f
            setTextColor(Color.rgb(35, 53, 80))
            setLineSpacing(0f, 1.25f)
        })

        root.addView(section("Đóng góp ý kiến"))
        root.addView(button("Gửi phản hồi qua email") {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:lienhe@mltudonghoa.pro.vn"))
            intent.putExtra(Intent.EXTRA_SUBJECT, "Góp ý ứng dụng ML Cún Cưng")
            runCatching { startActivity(intent) }.onFailure { toast("Điện thoại chưa có ứng dụng email phù hợp") }
        })
        return scroll
    }

    private fun openOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun startDog() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Cần cấp quyền hiển thị nổi trước")
            openOverlayPermission()
            return
        }
        val intent = Intent(this, DogOverlayService::class.java).setAction(DogOverlayService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        toast("Đã bật chú cún")
    }

    private fun stopDog() {
        startService(Intent(this, DogOverlayService::class.java).setAction(DogOverlayService.ACTION_STOP))
        toast("Đã tắt chú cún")
    }

    private fun scheduleReminder(audio: String?) {
        if (!Settings.canDrawOverlays(this)) {
            toast("Hãy cấp quyền hiển thị nổi để cún báo nhắc")
            return
        }
        val minutes = delayInput.text.toString().toLongOrNull()?.coerceIn(1, 10080) ?: 1L
        val message = messageInput.text.toString().ifBlank { "Đến giờ rồi!" }
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("message", message)
            audio?.let { putExtra("audio", it) }
        }
        val requestCode = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        val pending = PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            val formatted = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN")).format(Date(triggerAt))
            toast("Đã đặt lời nhắc lúc $formatted")
        } catch (_: Exception) {
            alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            toast("Đã đặt lời nhắc gần đúng")
        }
    }

    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC)
            return
        }
        if (recorder != null) return
        val file = File(filesDir, "giong_nhac_${System.currentTimeMillis()}.m4a")
        try {
            recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            currentRecording = file
            toast("Đang ghi âm…")
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            toast("Không thể ghi âm: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
    }

    private fun stopRecording() {
        val active = recorder ?: run { toast("Chưa bắt đầu ghi âm"); return }
        try {
            active.stop()
            currentRecording?.let { Prefs.setRecordingPath(this, it.absolutePath) }
            toast("Đã lưu giọng nói")
        } catch (_: Exception) {
            currentRecording?.delete()
            toast("Bản ghi quá ngắn, chưa lưu")
        } finally {
            active.release()
            recorder = null
            updateStatus()
        }
    }

    private fun playRecording() {
        val path = Prefs.recordingPath(this) ?: run { toast("Chưa có bản ghi"); return }
        playValue(path)
    }

    private fun pickAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_AUDIO)
    }

    private fun playSelectedAudio() {
        val uri = Prefs.audioUri(this) ?: run { toast("Chưa chọn âm thanh"); return }
        playValue(uri)
    }

    private fun playValue(value: String) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                if (value.startsWith("content://")) setDataSource(this@MainActivity, Uri.parse(value)) else setDataSource(value)
                setOnCompletionListener { it.release(); player = null }
                prepare()
                start()
            }
        } catch (e: Exception) {
            toast("Không phát được: ${e.message}")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_AUDIO && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            Prefs.setAudioUri(this, uri.toString())
            toast("Đã chọn âm thanh yêu thích")
            updateStatus()
        }
    }

    private fun updateStatus() {
        if (!::status.isInitialized) return
        val overlay = if (Settings.canDrawOverlays(this)) "Đã cấp" else "Chưa cấp"
        val record = if (Prefs.recordingPath(this) != null) "Có" else "Chưa có"
        val audio = if (Prefs.audioUri(this) != null) "Đã chọn" else "Chưa chọn"
        status.text = "Quyền nổi: $overlay   •   Giọng ghi: $record   •   Nhạc: $audio"
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onDestroy() {
        recorder?.release()
        player?.release()
        super.onDestroy()
    }

    private fun section(value: String) = TextView(this).apply {
        text = value
        textSize = 19f
        setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(Color.rgb(7, 59, 140))
        setPadding(0, dp(22), 0, dp(7))
    }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 16f
        setOnClickListener { action() }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(8) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
}
