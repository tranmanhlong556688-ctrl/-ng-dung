package vn.mltudonghoa.cuncung

import android.Manifest
import android.app.*
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
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var messageInput: EditText
    private lateinit var delayInput: EditText
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentRecording: File? = null

    private val audioPicker = registerForActivityResultCompat(4101) { data ->
        val uri = data?.data ?: return@registerForActivityResultCompat
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        Prefs.setAudioUri(this, uri.toString())
        toast("Đã chọn âm thanh yêu thích")
        updateStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNotificationPermission()
        FeedbackScheduler.ensureInitial(this)
        updateStatus()
        if (intent.getBooleanExtra("open_feedback", false)) status.postDelayed({ openFeedbackEmail() }, 500L)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("open_feedback", false) == true) openFeedbackEmail()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(245, 248, 255)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(40))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "🐶  ML Cún Cưng v2.0"
            textSize = 28f
            setTextColor(Color.rgb(7, 59, 140))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Semi 3D • 8 hướng • Chạy quanh icon • Nhân đôi"
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
        root.addView(button("2. Bật chú cún Semi 3D") { startDog() })
        root.addView(button("Tắt chú cún") { stopDog() })
        root.addView(checkBox("Cho cún tự chạy nhảy", Prefs.autoMove(this)) { Prefs.setAutoMove(this, it); refreshDog() })
        root.addView(checkBox("Tự bật lại sau khi khởi động điện thoại", Prefs.autoStart(this)) { Prefs.setAutoStart(this, it) })

        root.addView(section("Chất lượng và FPS"))
        root.addView(TextView(this).apply {
            text = "Cân bằng: 30 FPS khi chạy/chạm, 10–15 FPS khi nghỉ."
            setTextColor(Color.rgb(45, 62, 91))
        })
        val qualityLabels = arrayOf("Tiết kiệm pin", "Cân bằng", "Đẹp nhất")
        val qualityValues = arrayOf(Prefs.QUALITY_SAVER, Prefs.QUALITY_BALANCED, Prefs.QUALITY_BEST)
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, qualityLabels)
            setSelection(qualityValues.indexOf(Prefs.quality(this@MainActivity)).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    Prefs.setQuality(this@MainActivity, qualityValues[position]); refreshDog(); updateStatus()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }, matchWrap())
        root.addView(checkBox("Tự giảm hoạt ảnh khi pin còn 15%", Prefs.pauseLowBattery(this)) {
            Prefs.setPauseLowBattery(this, it); refreshDog()
        })

        root.addView(section("Vùng icon trên màn hình chính"))
        root.addView(TextView(this).apply {
            text = "Đánh dấu giữa các icon. Cún sẽ ưu tiên chạy vòng quanh, ngồi hoặc đánh hơi gần các điểm đó."
            setTextColor(Color.rgb(45, 62, 91))
        })
        root.addView(button("Đánh dấu / sửa vị trí icon") { startActivity(Intent(this, IconMarkerActivity::class.java)) })
        root.addView(button("Xóa toàn bộ điểm icon") {
            Prefs.setMarkers(this, emptyList()); updateStatus(); toast("Đã xóa các vùng icon")
        })

        root.addView(section("Hiệu ứng hai chú cún"))
        root.addView(checkBox("Bật hiệu ứng nhân đôi", Prefs.twinEnabled(this)) { Prefs.setTwinEnabled(this, it); refreshDog() })
        val intervalRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        intervalRow.addView(TextView(this).apply { text = "Khoảng cách giữa hai lần (phút): "; textSize = 15f })
        val twinMinutes = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(Prefs.twinIntervalMinutes(this@MainActivity).toString())
            setSelectAllOnFocus(true)
        }
        intervalRow.addView(twinMinutes, LinearLayout.LayoutParams(dp(80), -2))
        intervalRow.addView(Button(this).apply {
            text = "Lưu"; isAllCaps = false
            setOnClickListener {
                Prefs.setTwinIntervalMinutes(this@MainActivity, twinMinutes.text.toString().toIntOrNull() ?: 5)
                refreshDog(); toast("Đã lưu tần suất nhân đôi")
            }
        })
        root.addView(intervalRow, matchWrap())

        root.addView(TextView(this).apply { text = "Âm thanh phát sau khi hai cún nhập lại:" })
        val mergeLabels = arrayOf("Giọng vừa ghi", "Nhạc đã chọn", "Không phát")
        val mergeValues = arrayOf(Prefs.MERGE_AUDIO_RECORDING, Prefs.MERGE_AUDIO_SELECTED, Prefs.MERGE_AUDIO_NONE)
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, mergeLabels)
            setSelection(mergeValues.indexOf(Prefs.mergeAudio(this@MainActivity)).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    Prefs.setMergeAudio(this@MainActivity, mergeValues[position])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }, matchWrap())
        root.addView(button("Chạy thử hiệu ứng nhân đôi 3,5 giây") { sendDogAction(DogOverlayService.ACTION_TWIN_NOW) })

        root.addView(section("Giờ yên lặng"))
        val quietRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val quietStart = hourInput(Prefs.quietStart(this))
        val quietEnd = hourInput(Prefs.quietEnd(this))
        quietRow.addView(TextView(this).apply { text = "Từ " })
        quietRow.addView(quietStart, LinearLayout.LayoutParams(dp(68), -2))
        quietRow.addView(TextView(this).apply { text = " giờ đến " })
        quietRow.addView(quietEnd, LinearLayout.LayoutParams(dp(68), -2))
        quietRow.addView(TextView(this).apply { text = " giờ" })
        root.addView(quietRow)
        root.addView(button("Lưu giờ yên lặng") {
            Prefs.setQuietHours(this, quietStart.text.toString().toIntOrNull() ?: 22, quietEnd.text.toString().toIntOrNull() ?: 7)
            toast("Đã lưu giờ yên lặng")
        })

        root.addView(section("Nhắc việc"))
        messageInput = EditText(this).apply { hint = "Ví dụ: Đến giờ kiểm tra PCCC"; setText("Đến giờ thực hiện công việc rồi!") }
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

        root.addView(section("Góp ý và đề xuất nâng cấp"))
        root.addView(TextView(this).apply {
            text = "Lần đầu nhắc sau 24 giờ; lần thứ hai sau 30 ngày. Ứng dụng chỉ mở email đã điền sẵn, không tự gửi."
            setTextColor(Color.rgb(45, 62, 91))
        })
        root.addView(checkBox("Cho phép nhắc góp ý", Prefs.feedbackEnabled(this)) { enabled ->
            Prefs.setFeedbackEnabled(this, enabled)
            if (enabled) FeedbackScheduler.ensureInitial(this) else FeedbackScheduler.cancel(this)
        })
        root.addView(button("Mở mẫu góp ý đã điền sẵn") { openFeedbackEmail() })

        root.addView(section("Hướng dẫn Xiaomi Redmi 10"))
        root.addView(TextView(this).apply {
            text = "• Cấp quyền Cửa sổ nổi, Thông báo và Micro.\n• Pin → Không hạn chế cho ML Cún Cưng.\n• Bật Tự khởi chạy nếu muốn cún hoạt động sau khi mở máy.\n• Đánh dấu icon khi đang ở màn hình chính để điểm đặt chính xác.\n• Chạm cún để nghe lời động viên; kéo cún để đổi vị trí."
            textSize = 15f
            setTextColor(Color.rgb(35, 53, 80))
            setLineSpacing(0f, 1.25f)
        })
        return scroll
    }

    private fun openOverlayPermission() = startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

    private fun startDog() {
        if (!Settings.canDrawOverlays(this)) { toast("Cần cấp quyền hiển thị nổi trước"); openOverlayPermission(); return }
        sendDogAction(DogOverlayService.ACTION_START)
        toast("Đã bật chú cún Semi 3D")
        updateStatus()
    }

    private fun stopDog() {
        startService(Intent(this, DogOverlayService::class.java).setAction(DogOverlayService.ACTION_STOP))
        toast("Đã tắt chú cún")
    }

    private fun refreshDog() = sendDogAction(DogOverlayService.ACTION_REFRESH)

    private fun sendDogAction(action: String) {
        val intent = Intent(this, DogOverlayService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun scheduleReminder(audio: String?) {
        if (!Settings.canDrawOverlays(this)) { toast("Hãy cấp quyền hiển thị nổi để cún báo nhắc"); return }
        val minutes = delayInput.text.toString().toLongOrNull()?.coerceIn(1, 10080) ?: 1L
        val message = messageInput.text.toString().ifBlank { "Đến giờ rồi!" }
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("message", message); audio?.let { putExtra("audio", it) }
        }
        val requestCode = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        val pending = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            toast("Đã đặt lời nhắc lúc ${SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN")).format(Date(triggerAt))}")
        } catch (_: Exception) {
            alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pending); toast("Đã đặt lời nhắc gần đúng")
        }
    }

    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2001); return
        }
        if (recorder != null) return
        val file = File(filesDir, "giong_nhac_${System.currentTimeMillis()}.m4a")
        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare(); start()
            }
            currentRecording = file; toast("Đang ghi âm…")
        } catch (e: Exception) {
            recorder?.release(); recorder = null; toast("Không thể ghi âm: ${e.message}")
        }
    }

    private fun stopRecording() {
        val active = recorder ?: run { toast("Chưa bắt đầu ghi âm"); return }
        try {
            active.stop(); currentRecording?.let { Prefs.setRecordingPath(this, it.absolutePath) }; toast("Đã lưu giọng nói")
        } catch (_: Exception) {
            currentRecording?.delete(); toast("Bản ghi quá ngắn, chưa lưu")
        } finally {
            active.release(); recorder = null; updateStatus()
        }
    }

    private fun playRecording() {
        val path = Prefs.recordingPath(this) ?: run { toast("Chưa có bản ghi"); return }; playValue(path)
    }

    private fun pickAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        audioPicker.launch(intent)
    }

    private fun playSelectedAudio() {
        val uri = Prefs.audioUri(this) ?: run { toast("Chưa chọn âm thanh"); return }; playValue(uri)
    }

    private fun playValue(value: String) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                if (value.startsWith("content://")) setDataSource(this@MainActivity, Uri.parse(value)) else setDataSource(value)
                setOnCompletionListener { it.release(); player = null }
                prepare(); start()
            }
        } catch (e: Exception) { toast("Không phát được: ${e.message}") }
    }

    private fun openFeedbackEmail() {
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrDefault("2.0.0")
        val body = """
            Xin chào ML Tự Động Hóa,

            Tôi gửi góp ý cho ứng dụng ML Cún Cưng.

            Phiên bản ứng dụng: $version
            Thiết bị: ${Build.MANUFACTURER} ${Build.MODEL}
            Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Chế độ hình ảnh: ${qualityLabel(Prefs.quality(this))}
            Số vùng icon đã đánh dấu: ${Prefs.markers(this).size}

            Mức độ hài lòng (1–5):
            Chức năng thường sử dụng:
            Lỗi đã gặp:
            Chức năng muốn bổ sung:
            Góp ý khác:

            Lưu ý: Hãy kiểm tra nội dung trước khi bấm Gửi.
        """.trimIndent()
        val uri = Uri.parse("mailto:lienhe@mltudonghoa.pro.vn?subject=${Uri.encode("Góp ý ML Cún Cưng v2.0")}&body=${Uri.encode(body)}")
        runCatching { startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
            .onFailure { toast("Chưa tìm thấy ứng dụng email trên điện thoại") }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2002)
        }
    }

    private fun updateStatus() {
        val overlay = if (Settings.canDrawOverlays(this)) "Đã cấp" else "Chưa cấp"
        val record = if (Prefs.recordingPath(this) != null) "Có" else "Chưa có"
        val audio = if (Prefs.audioUri(this) != null) "Đã chọn" else "Chưa chọn"
        status.text = "Quyền nổi: $overlay • Giọng: $record • Nhạc: $audio\nChất lượng: ${qualityLabel(Prefs.quality(this))} • Icon: ${Prefs.markers(this).size} điểm"
    }

    private fun qualityLabel(value: String) = when (value) {
        Prefs.QUALITY_SAVER -> "Tiết kiệm pin"
        Prefs.QUALITY_BEST -> "Đẹp nhất"
        else -> "Cân bằng"
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) updateStatus() }
    override fun onDestroy() { recorder?.release(); player?.release(); super.onDestroy() }

    private fun section(text: String) = TextView(this).apply {
        this.text = text; textSize = 19f; setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(Color.rgb(7, 59, 140)); setPadding(0, dp(22), 0, dp(7))
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 16f; setOnClickListener { action() }
    }

    private fun checkBox(text: String, checked: Boolean, changed: (Boolean) -> Unit) = CheckBox(this).apply {
        this.text = text; isChecked = checked; setOnCheckedChangeListener { _, value -> changed(value) }
    }

    private fun hourInput(value: Int) = EditText(this).apply {
        inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(value.toString()); setSelectAllOnFocus(true)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    private fun registerForActivityResultCompat(requestCode: Int, callback: (Intent?) -> Unit): SimpleResultLauncher {
        return SimpleResultLauncher(this, requestCode, callback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SimpleResultLauncher.dispatch(requestCode, resultCode, data)
    }
}

private class SimpleResultLauncher(
    private val activity: Activity,
    private val requestCode: Int,
    callback: (Intent?) -> Unit
) {
    init { callbacks[requestCode] = callback }
    fun launch(intent: Intent) = activity.startActivityForResult(intent, requestCode)

    companion object {
        private val callbacks = mutableMapOf<Int, (Intent?) -> Unit>()
        fun dispatch(requestCode: Int, resultCode: Int, data: Intent?) {
            if (resultCode == Activity.RESULT_OK) callbacks[requestCode]?.invoke(data)
        }
    }
}
