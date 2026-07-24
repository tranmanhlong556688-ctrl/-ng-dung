package com.minh.autotouch.redmilite

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var config: LiteConfig
    private lateinit var statusText: TextView
    private lateinit var targetText: TextView
    private lateinit var loopEdit: EditText
    private lateinit var initialDelayEdit: EditText
    private lateinit var intervalEdit: EditText
    private val pointChecks = mutableListOf<CheckBox>()
    private val pointTexts = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = LiteStore.load(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(30))
            setBackgroundColor(Color.WHITE)
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Auto Touch Minh v1.3 – REDMI LITE"
            textSize = 23f
            setTextColor(Color.rgb(13, 71, 161))
            setPadding(0, 0, 0, dp(6))
        })
        root.addView(TextView(this).apply {
            text = "Tối ưu riêng cho Redmi 10 • MIUI 14.0.7 • Android 13 • RAM 4 GB"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, dp(10))
        })
        root.addView(TextView(this).apply {
            text = "Bản này đã bỏ cửa sổ nổi, dịch vụ chạy nền, vuốt, nhấn giữ, nhật ký và xuất file. Chỉ giữ tối đa 3 điểm chạm để giảm xung đột với MIUI."
            textSize = 14f
            setTextColor(Color.rgb(120, 70, 0))
            setBackgroundColor(Color.rgb(255, 248, 225))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }, fullWidth())

        statusText = TextView(this).apply {
            textSize = 15f
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.rgb(232, 240, 254))
        }
        root.addView(statusText, fullWidth(top = 10))

        root.addView(sectionTitle("1. Quyền Trợ năng tối thiểu"))
        val permissionRow = horizontalRow()
        permissionRow.addView(button("MỞ TRỢ NĂNG") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, weighted())
        permissionRow.addView(button("MIUI 14 CHẶN QUYỀN") { showMiuiHelp() }, weighted())
        root.addView(permissionRow)

        root.addView(sectionTitle("2. Chọn ứng dụng mục tiêu"))
        targetText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.rgb(245, 245, 245))
        }
        updateTargetText()
        root.addView(targetText, fullWidth())
        root.addView(button("CHỌN ỨNG DỤNG TRÊN MÁY") { showAppPicker() }, fullWidth(top = 8))

        root.addView(sectionTitle("3. Chọn tối đa 3 điểm chạm"))
        repeat(3) { index -> root.addView(createPointRow(index)) }

        root.addView(sectionTitle("4. Thời gian và số vòng"))
        loopEdit = numberField(config.loopCount.toString(), "Số vòng 1–100")
        initialDelayEdit = numberField((config.initialDelayMs / 1000L).toString(), "Chờ trước khi chạy 1–15 giây")
        intervalEdit = numberField(config.intervalMs.toString(), "Khoảng cách giữa các điểm 200–5000 ms")
        root.addView(label("Số vòng lặp (1–100)")); root.addView(loopEdit, fullWidth())
        root.addView(label("Chờ sau khi mở ứng dụng mục tiêu (giây)")); root.addView(initialDelayEdit, fullWidth())
        root.addView(label("Khoảng cách giữa các lần chạm (ms)")); root.addView(intervalEdit, fullWidth())

        root.addView(sectionTitle("5. Điều khiển"))
        root.addView(button("LƯU VÀ BẮT ĐẦU") { startAutomation() }, fullWidth())
        root.addView(button("DỪNG NGAY") {
            LiteAccessibilityService.stopNow()
            toast("Đã gửi lệnh dừng")
        }, fullWidth(top = 8))

        root.addView(sectionTitle("Cách dùng"))
        root.addView(TextView(this).apply {
            text = "1) Bật Trợ năng cho Auto Touch Minh Redmi Lite.\n" +
                "2) Chọn ứng dụng mục tiêu.\n" +
                "3) Bật điểm cần dùng và bấm CHỌN VỊ TRÍ.\n" +
                "4) Đặt số vòng và thời gian.\n" +
                "5) Bấm LƯU VÀ BẮT ĐẦU. Ứng dụng mục tiêu sẽ tự mở.\n\n" +
                "Ứng dụng tự dừng khi bạn chuyển sang ứng dụng khác hoặc khóa màn hình. Không dùng trong ngân hàng, CAPTCHA, OTP hoặc màn hình mật khẩu."
            textSize = 15f
            setLineSpacing(0f, 1.15f)
        })
    }

    private fun createPointRow(index: Int): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(if (index % 2 == 0) Color.rgb(248, 249, 250) else Color.WHITE)
        }
        val top = horizontalRow()
        val check = CheckBox(this).apply {
            text = "Bật Điểm ${index + 1}"
            isChecked = config.points[index].enabled
        }
        pointChecks.add(check)
        top.addView(check, weighted())
        top.addView(button("CHỌN VỊ TRÍ") { openCalibration(index) })
        box.addView(top)

        val text = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        pointTexts.add(text)
        box.addView(text)
        updatePointText(index)
        return box
    }

    private fun openCalibration(index: Int) {
        syncConfigFromUi(showErrors = false)
        val point = config.points[index]
        startActivityForResult(
            Intent(this, CalibrationActivity::class.java)
                .putExtra("x", point.x)
                .putExtra("y", point.y),
            300 + index
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode in 300..302 && resultCode == RESULT_OK && data != null) {
            val index = requestCode - 300
            config.points[index].x = data.getIntExtra("x", config.points[index].x)
            config.points[index].y = data.getIntExtra("y", config.points[index].y)
            config.points[index].enabled = true
            pointChecks[index].isChecked = true
            updatePointText(index)
            LiteStore.save(this, config)
        }
    }

    private fun showAppPicker() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .map { info ->
                Triple(
                    info.loadLabel(packageManager).toString(),
                    info.activityInfo.packageName,
                    info
                )
            }
            .filter { it.second != packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }

        if (apps.isEmpty()) return toast("Không tìm thấy ứng dụng có thể mở")
        val items = apps.map { "${it.first}\n${it.second}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Chọn ứng dụng được phép")
            .setItems(items) { _, which ->
                config.targetLabel = apps[which].first
                config.targetPackage = apps[which].second
                updateTargetText()
                LiteStore.save(this, config)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun startAutomation() {
        if (!syncConfigFromUi(showErrors = true)) return
        if (!isAccessibilityEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("Chưa bật Trợ năng")
                .setMessage("Hãy bật dịch vụ Auto Touch Minh Redmi Lite rồi quay lại ứng dụng.")
                .setPositiveButton("Mở cài đặt") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("Hủy", null)
                .show()
            return
        }
        if (!LiteAccessibilityService.isConnected()) {
            toast("Trợ năng đang khởi động. Hãy chờ 2 giây rồi bấm lại")
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(config.targetPackage)
        if (launchIntent == null) return toast("Không thể mở ứng dụng mục tiêu")

        LiteStore.save(this, config)
        LiteAccessibilityService.prepare(config)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(launchIntent)
    }

    private fun syncConfigFromUi(showErrors: Boolean): Boolean {
        config.loopCount = loopEdit.text.toString().toIntOrNull()?.coerceIn(1, 100) ?: 5
        config.initialDelayMs = ((initialDelayEdit.text.toString().toLongOrNull() ?: 3L).coerceIn(1L, 15L)) * 1000L
        config.intervalMs = (intervalEdit.text.toString().toLongOrNull() ?: 600L).coerceIn(200L, 5000L)
        config.points.forEachIndexed { index, point -> point.enabled = pointChecks[index].isChecked }

        if (config.targetPackage.isBlank()) {
            if (showErrors) toast("Hãy chọn ứng dụng mục tiêu")
            return false
        }
        if (config.points.none { it.enabled }) {
            if (showErrors) toast("Hãy bật ít nhất một điểm")
            return false
        }
        LiteStore.save(this, config)
        return true
    }

    private fun isAccessibilityEnabled(): Boolean {
        val component = ComponentName(this, LiteAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any {
            it.equals(component.flattenToString(), ignoreCase = true) ||
                it.equals(component.flattenToShortString(), ignoreCase = true)
        }
    }

    private fun showMiuiHelp() {
        AlertDialog.Builder(this)
            .setTitle("Redmi 10 • MIUI 14 • Android 13")
            .setMessage(
                "Nếu công tắc Trợ năng bị mờ hoặc không bật được:\n\n" +
                    "1. Mở THÔNG TIN ỨNG DỤNG.\n" +
                    "2. Bấm dấu ba chấm ở góc trên.\n" +
                    "3. Chọn CHO PHÉP CÀI ĐẶT BỊ HẠN CHẾ nếu mục này xuất hiện.\n" +
                    "4. Quay lại: Cài đặt > Cài đặt bổ sung > Trợ năng > Ứng dụng đã tải xuống.\n" +
                    "5. Bật Auto Touch Minh Redmi Lite.\n\n" +
                    "Không cần tắt Tối ưu hóa MIUI, Play Protect hoặc ứng dụng Bảo mật."
            )
            .setPositiveButton("Thông tin ứng dụng") { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:$packageName"))
                )
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun refreshStatus() {
        val accessibility = isAccessibilityEnabled()
        statusText.text = if (accessibility) {
            "✓ Trợ năng đã bật • Không dùng cửa sổ nổi • Không chạy dịch vụ nền"
        } else {
            "⚠ Chưa bật Trợ năng. Ứng dụng vẫn mở được nhưng chưa thể tự chạm."
        }
        statusText.setTextColor(if (accessibility) Color.rgb(0, 105, 55) else Color.rgb(170, 70, 0))
    }

    private fun updateTargetText() {
        targetText.text = "${config.targetLabel}\n${config.targetPackage.ifBlank { "Chưa có mã gói" }}"
    }

    private fun updatePointText(index: Int) {
        val p = config.points[index]
        pointTexts[index].text = "X = ${p.x}   •   Y = ${p.y}"
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(Color.rgb(13, 71, 161))
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(Color.DKGRAY)
        setPadding(0, dp(8), 0, dp(4))
    }

    private fun numberField(value: String, hintValue: String) = EditText(this).apply {
        setText(value)
        hint = hintValue
        inputType = InputType.TYPE_CLASS_NUMBER
        setPadding(dp(10), dp(8), dp(10), dp(8))
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun horizontalRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun weighted() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun fullWidth(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
