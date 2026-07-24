package com.minh.autotouch

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var allowedPackagesEdit: EditText
    private lateinit var config: AppConfig
    private val stepSummaryViews = mutableListOf<TextView>()

    private val requestExportConfig = 201
    private val requestImportConfig = 202
    private val requestExportLog = 203

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = ConfigStore.load(this)
        buildUi()
        if (!ConsentStore.isAccepted(this)) {
            root.post { showAccessibilityDisclosure(openSettingsAfter = false) }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshStepSummaries()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(30))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Auto Touch Minh v1.2.1 – Chế độ an toàn"
            textSize = 23f
            setTextColor(Color.rgb(13, 71, 161))
            setPadding(0, 0, 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "Ứng dụng chỉ chạy kịch bản cố định do bạn tự tạo, trong đúng ứng dụng bạn cho phép. Không đọc chữ trên màn hình, không ghi mật khẩu và không gửi dữ liệu ra mạng."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, dp(10))
        })

        statusText = TextView(this).apply {
            textSize = 14f
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.rgb(232, 240, 254))
        }
        root.addView(statusText, fullWidth())

        root.addView(sectionTitle("1. Thông báo quyền và cài đặt Android"))
        root.addView(TextView(this).apply {
            text = "Quyền Trợ năng được dùng duy nhất để thực hiện nhấp, nhấn giữ và vuốt tại tọa độ bạn đã đặt. Dịch vụ chỉ nhận tên gói ứng dụng đang mở để ngăn thao tác chạy sang ứng dụng khác. Bạn có thể tắt quyền bất cứ lúc nào trong Cài đặt Android."
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.rgb(255, 248, 225))
        }, fullWidth())

        val permissionRow = horizontalWrap()
        permissionRow.addView(button("Cấp quyền nổi") { openOverlayPermission() })
        permissionRow.addView(button("Đọc và bật Trợ năng") { showAccessibilityDisclosure(true) })
        permissionRow.addView(button("Nếu Android chặn") { showRestrictedSettingsHelp() })
        root.addView(permissionRow)

        root.addView(sectionTitle("2. Chỉ định ứng dụng được phép"))
        root.addView(TextView(this).apply {
            text = "Bắt buộc chọn ít nhất một ứng dụng. Auto Touch sẽ dừng ngay khi màn hình chuyển sang ứng dụng khác."
            setTextColor(Color.DKGRAY)
        })
        allowedPackagesEdit = edit(config.allowedPackagesRaw, false).apply {
            hint = "Ví dụ: com.congty.ungdung"
        }
        root.addView(allowedPackagesEdit, fullWidth())
        val appRow = horizontalWrap()
        appRow.addView(button("Chọn ứng dụng") { showAppPicker() })
        appRow.addView(button("Xóa danh sách") { allowedPackagesEdit.setText("") })
        root.addView(appRow)

        root.addView(sectionTitle("3. Cấu hình chu trình"))
        val profileField = labeledEdit("Tên cấu hình", config.profileName, false)
        val loopField = labeledEdit("Số vòng (1–999)", config.loopCount.toString(), true)
        val loopDelayField = labeledEdit("Chờ giữa vòng (ms, tối thiểu 200)", config.loopDelayMs.toString(), true)
        val countdownField = labeledEdit("Đếm ngược (giây, tối thiểu 1)", config.countdownSec.toString(), true)
        val markerSizeField = labeledEdit("Kích thước điểm (dp)", config.markerSizeDp.toString(), true)
        val alphaField = labeledEdit("Độ rõ điểm (20–100%)", config.markerAlphaPercent.toString(), true)

        root.addView(profileField.first); root.addView(profileField.second)
        root.addView(loopField.first); root.addView(loopField.second)
        root.addView(loopDelayField.first); root.addView(loopDelayField.second)
        root.addView(countdownField.first); root.addView(countdownField.second)
        root.addView(markerSizeField.first); root.addView(markerSizeField.second)
        root.addView(alphaField.first); root.addView(alphaField.second)

        root.addView(button("Lưu cài đặt chung") {
            config.profileName = profileField.second.text.toString().ifBlank { "Cấu hình mặc định" }
            config.allowedPackagesRaw = allowedPackagesEdit.text.toString().trim()
            config.loopCount = loopField.second.intValue(1).coerceIn(1, 999)
            config.loopDelayMs = loopDelayField.second.longValue(500).coerceAtLeast(200)
            config.countdownSec = countdownField.second.intValue(3).coerceIn(1, 30)
            config.markerSizeDp = markerSizeField.second.intValue(46).coerceIn(28, 90)
            config.markerAlphaPercent = alphaField.second.intValue(80).coerceIn(20, 100)
            ConfigStore.save(this, config)
            toast("Đã lưu cài đặt chung")
            refreshOverlay()
        }, fullWidth())

        root.addView(sectionTitle("4. Các điểm thao tác"))
        for (i in 0 until 10) root.addView(createStepRow(i))

        root.addView(sectionTitle("5. Điều khiển"))
        val controlRow = horizontalWrap()
        controlRow.addView(button("Hiện điểm nổi") { startOverlay() })
        controlRow.addView(button("Nạp lại điểm") { refreshOverlay() })
        controlRow.addView(button("Tắt điểm nổi") { stopService(Intent(this, OverlayService::class.java)) })
        root.addView(controlRow)
        root.addView(TextView(this).apply {
            text = "Dừng khẩn cấp: bấm nút ■ trên bảng điều khiển nổi. Ứng dụng cũng tự dừng khi khóa màn hình hoặc chuyển khỏi ứng dụng được phép."
            setPadding(0, dp(6), 0, dp(8))
            setTextColor(Color.rgb(170, 0, 0))
        })

        root.addView(sectionTitle("6. Sao lưu và nhật ký"))
        val ioRow = horizontalWrap()
        ioRow.addView(button("Xuất JSON") { exportConfig() })
        ioRow.addView(button("Nhập JSON") { importConfig() })
        ioRow.addView(button("Xuất log CSV") { exportLog() })
        ioRow.addView(button("Xóa log") {
            AlertDialog.Builder(this).setTitle("Xóa nhật ký?")
                .setPositiveButton("Xóa") { _, _ -> LogStore.clear(this); toast("Đã xóa nhật ký") }
                .setNegativeButton("Hủy", null).show()
        })
        root.addView(ioRow)

        root.addView(sectionTitle("7. Hướng dẫn nhanh"))
        root.addView(TextView(this).apply {
            text = "1) Chọn ứng dụng được phép và lưu cài đặt.\n2) Cấp quyền hiển thị nổi.\n3) Đọc thông báo và tự bật Dịch vụ trợ năng.\n4) Bật các điểm cần dùng, kéo chúng đến vị trí cần thao tác.\n5) Mở ứng dụng đã chọn rồi nhấn ▶.\n\nAndroid 13 trở lên: nếu mục Trợ năng bị làm mờ, mở Thông tin ứng dụng > dấu ba chấm > Cho phép cài đặt bị hạn chế. Đây là bước bảo vệ bắt buộc của Android đối với ứng dụng cài ngoài cửa hàng.\n\nXiaomi/Redmi: đặt Pin thành Không hạn chế và chỉ bật Tự khởi động khi thật sự cần."
            textSize = 15f
            setLineSpacing(0f, 1.15f)
        })
    }

    private fun createStepRow(index: Int): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(if (index % 2 == 0) Color.rgb(248, 249, 250) else Color.WHITE)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val enabled = CheckBox(this).apply {
            text = "Điểm ${index + 1}"
            isChecked = config.steps[index].enabled
            setOnCheckedChangeListener { _, checked ->
                config.steps[index].enabled = checked
                ConfigStore.save(this@MainActivity, config)
                refreshStepSummaries()
                refreshOverlay()
            }
        }
        top.addView(enabled, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(button("Chỉnh sửa") { showStepEditor(index) })
        container.addView(top)
        val summary = TextView(this).apply {
            setPadding(dp(8), 0, dp(8), dp(6))
            textSize = 13f
            setTextColor(Color.DKGRAY)
        }
        stepSummaryViews.add(summary)
        container.addView(summary)
        return container
    }

    private fun showStepEditor(index: Int) {
        val step = config.steps[index]
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(6), dp(18), dp(6))
        }
        scroll.addView(box)

        val name = edit(step.name, false)
        box.addView(label("Tên/ghi chú")); box.addView(name)

        box.addView(label("Loại thao tác"))
        val spinner = Spinner(this)
        val labels = ActionType.values().map { it.label }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinner.setSelection(step.action.ordinal.coerceIn(0, ActionType.values().lastIndex))
        box.addView(spinner)

        val x = field(box, "Tọa độ X", step.x)
        val y = field(box, "Tọa độ Y", step.y)
        val endX = field(box, "Tọa độ đích X (vuốt tùy chỉnh)", step.endX)
        val endY = field(box, "Tọa độ đích Y (vuốt tùy chỉnh)", step.endY)
        val repeat = field(box, "Số lần nhấp (1–100)", step.repeatCount)
        val pre = field(box, "Chờ trước thao tác (ms)", step.preDelayMs)
        val interval = field(box, "Khoảng cách giữa lần nhấp (ms, tối thiểu 120)", step.intervalMs)
        val duration = field(box, "Thời gian nhấn/vuốt/chờ (ms)", step.durationMs)
        val post = field(box, "Chờ sau thao tác (ms)", step.postDelayMs)

        box.addView(TextView(this).apply {
            text = "Tọa độ X/Y tự cập nhật khi bạn kéo điểm nổi."
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, 0)
        })

        AlertDialog.Builder(this)
            .setTitle("Cài đặt Điểm ${index + 1}")
            .setView(scroll)
            .setPositiveButton("Lưu") { _, _ ->
                step.name = name.text.toString().ifBlank { "Điểm ${index + 1}" }
                step.action = ActionType.values()[spinner.selectedItemPosition]
                step.x = x.intValue(step.x).coerceAtLeast(0)
                step.y = y.intValue(step.y).coerceAtLeast(0)
                step.endX = endX.intValue(step.endX).coerceAtLeast(0)
                step.endY = endY.intValue(step.endY).coerceAtLeast(0)
                step.repeatCount = repeat.intValue(1).coerceIn(1, 100)
                step.preDelayMs = pre.longValue(300).coerceAtLeast(0)
                step.intervalMs = interval.longValue(250).coerceAtLeast(120)
                step.durationMs = duration.longValue(500).coerceIn(50, 60_000)
                step.postDelayMs = post.longValue(300).coerceAtLeast(0)
                ConfigStore.save(this, config)
                refreshStepSummaries()
                refreshOverlay()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAccessibilityDisclosure(openSettingsAfter: Boolean) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        content.addView(TextView(this).apply {
            text = "Auto Touch Minh sử dụng Android AccessibilityService để thực hiện đúng các nhấp và vuốt bạn đã cài đặt. Dịch vụ nhận tên gói ứng dụng đang mở để chỉ cho phép chạy trong danh sách bạn chọn. Ứng dụng không đọc nội dung cửa sổ, không thu thập mật khẩu, không tự đưa ra quyết định và không chia sẻ dữ liệu. Tất cả cấu hình và nhật ký được lưu cục bộ. Bạn có thể dừng hoặc tắt quyền bất cứ lúc nào."
            textSize = 15f
        })
        val checkbox = CheckBox(this).apply {
            text = "Tôi đã đọc, hiểu và đồng ý bật quyền này"
            setPadding(0, dp(12), 0, 0)
            isChecked = ConsentStore.isAccepted(this@MainActivity)
        }
        content.addView(checkbox)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Thông báo sử dụng quyền Trợ năng")
            .setView(content)
            .setPositiveButton("Đồng ý", null)
            .setNegativeButton("Để sau", null)
            .create()
        dialog.setOnShowListener {
            val accept = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            accept.isEnabled = checkbox.isChecked
            checkbox.setOnCheckedChangeListener { _, checked -> accept.isEnabled = checked }
            accept.setOnClickListener {
                ConsentStore.setAccepted(this, true)
                dialog.dismiss()
                refreshStatus()
                if (openSettingsAfter) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        dialog.show()
    }

    private fun showRestrictedSettingsHelp() {
        AlertDialog.Builder(this)
            .setTitle("Khi Android chặn quyền Trợ năng")
            .setMessage("Trên Android 13 trở lên, ứng dụng cài từ file APK có thể bị khóa mục Trợ năng. Hãy mở Thông tin ứng dụng Auto Touch Minh, bấm dấu ba chấm ở góc trên và chọn ‘Cho phép cài đặt bị hạn chế’. Sau đó quay lại và tự bật Dịch vụ trợ năng. Không tắt Play Protect và không dùng công cụ vượt bảo vệ.")
            .setPositiveButton("Mở thông tin ứng dụng") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun showAppPicker() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val entries = packageManager.queryIntentActivities(intent, 0)
            .map { info ->
                val label = info.loadLabel(packageManager).toString()
                val pkg = info.activityInfo.packageName
                Triple(label, pkg, "$label\n$pkg")
            }
            .filter { it.second != packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }

        if (entries.isEmpty()) {
            toast("Không tìm thấy ứng dụng có biểu tượng khởi chạy")
            return
        }
        val labels = entries.map { it.third }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Chọn ứng dụng được phép")
            .setItems(labels) { _, which ->
                val selected = entries[which].second
                val current = allowedPackagesEdit.text.toString()
                    .split(',', ';', '\n', ' ')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableSet()
                current.add(selected)
                allowedPackagesEdit.setText(current.joinToString(", "))
                toast("Đã thêm $selected")
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun refreshStepSummaries() {
        config = ConfigStore.load(this)
        stepSummaryViews.forEachIndexed { i, view ->
            val s = config.steps[i]
            view.text = "${if (s.enabled) "ĐANG BẬT" else "Đã tắt"} • ${s.action.label} • (${s.x}, ${s.y}) • trước ${s.preDelayMs}ms • sau ${s.postDelayMs}ms"
        }
    }

    private fun refreshStatus() {
        val overlay = Settings.canDrawOverlays(this)
        val accessibility = AutomationAccessibilityService.instance != null
        val consent = ConsentStore.isAccepted(this)
        statusText.text = "Quyền nổi: ${if (overlay) "Đã cấp" else "Chưa cấp"}   |   Trợ năng: ${if (accessibility) "Đã bật" else "Chưa bật"}   |   Đồng ý: ${if (consent) "Có" else "Chưa"}"
    }

    private fun startOverlay() {
        config = ConfigStore.load(this)
        if (!ConsentStore.isAccepted(this)) {
            showAccessibilityDisclosure(false)
            return
        }
        if (config.allowedPackages().isEmpty()) {
            toast("Hãy chọn ứng dụng được phép và bấm Lưu cài đặt chung")
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            openOverlayPermission()
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        toast("Đã mở bảng điều khiển nổi")
    }

    private fun refreshOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        val intent = Intent(this, OverlayService::class.java).setAction("REFRESH")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun openOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun exportConfig() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "AutoTouchMinh_config.json")
        }, requestExportConfig)
    }

    private fun importConfig() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }, requestImportConfig)
    }

    private fun exportLog() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "AutoTouchMinh_log.csv")
        }, requestExportLog)
    }

    @Deprecated("Deprecated in Android API but compatible with minSdk 26")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data ?: return
        when (requestCode) {
            requestExportConfig -> runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(ConfigStore.exportText(this)) }
            }.onSuccess { toast("Đã xuất cấu hình") }.onFailure { toast("Lỗi xuất: ${it.message}") }
            requestImportConfig -> runCatching {
                val text = contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: error("Không đọc được file")
                config = ConfigStore.importText(this, text)
            }.onSuccess {
                toast("Đã nhập cấu hình")
                recreate()
                refreshOverlay()
            }.onFailure { toast("File không hợp lệ: ${it.message}") }
            requestExportLog -> runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(LogStore.read(this)) }
            }.onSuccess { toast("Đã xuất nhật ký") }.onFailure { toast("Lỗi xuất log: ${it.message}") }
        }
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(Color.rgb(13, 71, 161))
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setPadding(0, dp(8), 0, dp(2))
    }

    private fun labeledEdit(label: String, value: String, numeric: Boolean): Pair<TextView, EditText> =
        Pair(this.label(label), edit(value, numeric))

    private fun field(parent: LinearLayout, label: String, value: Number): EditText {
        parent.addView(this.label(label))
        return edit(value.toString(), true).also { parent.addView(it) }
    }

    private fun edit(value: String, numeric: Boolean): EditText = EditText(this).apply {
        setText(value)
        inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        setSelectAllOnFocus(true)
    }

    private fun button(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        setOnClickListener { onClick() }
    }

    private fun horizontalWrap() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.START
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    private fun EditText.intValue(default: Int): Int = text.toString().toIntOrNull() ?: default
    private fun EditText.longValue(default: Long): Long = text.toString().toLongOrNull() ?: default
}
