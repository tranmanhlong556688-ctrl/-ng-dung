package vn.minh.lgirremote

import android.app.*
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import vn.minh.lgirremote.ir.IrTransmitter
import vn.minh.lgirremote.ir.LgAcProtocol
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var ir: IrTransmitter
    private val prefs by lazy { getSharedPreferences("lg_ir_remote", MODE_PRIVATE) }
    private lateinit var irStatus: TextView
    private lateinit var scanStatus: TextView
    private lateinit var stateStatus: TextView
    private lateinit var timerStatus: TextView
    private lateinit var scanPanel: LinearLayout
    private lateinit var controlPanel: LinearLayout
    private lateinit var powerButton: Button
    private var candidateIndex = 0
    private var selectedProfile: LgAcProtocol.Profile? = null
    private var state = LgAcProtocol.State()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ir = IrTransmitter(this)
        loadSavedState()
        setContentView(buildUi())
        refreshUi()
    }

    override fun onResume() { super.onResume(); if (::timerStatus.isInitialized) refreshUi() }

    private fun loadSavedState() {
        candidateIndex = prefs.getInt("candidate_index", 0).coerceIn(0, LgAcProtocol.scanCandidates.lastIndex)
        selectedProfile = LgAcProtocol.profileById(prefs.getString("profile_id", null))
        state = LgAcProtocol.State(
            power = prefs.getBoolean("power", false),
            temperature = prefs.getInt("temperature", 24).coerceIn(16, 30),
            mode = enumValueOrDefault(prefs.getString("mode", null), LgAcProtocol.Mode.COOL),
            fan = enumValueOrDefault(prefs.getString("fan", null), selectedProfile?.preferredFan ?: LgAcProtocol.Fan.AUTO),
            swing = prefs.getBoolean("swing", false)
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)

    private fun buildUi(): View {
        window.statusBarColor = Color.rgb(3, 8, 20)
        window.navigationBarColor = Color.rgb(3, 8, 20)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(30))
            setBackgroundColor(Color.rgb(3, 8, 20))
        }
        root.addView(TextView(this).apply {
            text = "LG  •  NEXUS REMOTE"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(90, 220, 255))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "BỘ ĐIỀU KHIỂN HỒNG NGOẠI V2.0"
            textSize = 12f
            letterSpacing = .14f
            setTextColor(Color.rgb(120, 145, 175))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        })
        irStatus = label(15f); root.addView(irStatus)

        scanPanel = section("QUÉT & DÒ MÃ LG")
        scanStatus = label(17f); scanPanel.addView(scanStatus)
        scanPanel.addView(infoText("Giữ đầu phát IR của Redmi 10 hướng vào điều hòa, cách khoảng 1–3 m. Phát thử từng mã và lưu mã khi điều hòa phản hồi."))
        scanPanel.addView(actionButton("PHÁT MÃ THỬ", true) { transmitCurrentCandidate() })
        scanPanel.addView(horizontalRow(actionButton("← MÃ TRƯỚC") { changeCandidate(-1) }, actionButton("MÃ TIẾP →") { changeCandidate(1) }))
        scanPanel.addView(actionButton("✓ LƯU MÃ ĐANG HOẠT ĐỘNG", true) { saveCurrentCandidate() })
        root.addView(scanPanel)

        controlPanel = section("TRẠNG THÁI ĐIỀU HÒA")
        stateStatus = label(19f).apply { gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD }
        controlPanel.addView(stateStatus)
        powerButton = actionButton("BẬT", true) { state = state.copy(power = !state.power); sendState("Nguồn") }
        controlPanel.addView(powerButton)
        controlPanel.addView(horizontalRow(
            actionButton("−  NHIỆT ĐỘ", true) { state = state.copy(temperature = (state.temperature - 1).coerceAtLeast(16), power = true); sendState("Giảm nhiệt độ") },
            actionButton("+  NHIỆT ĐỘ", true) { state = state.copy(temperature = (state.temperature + 1).coerceAtMost(30), power = true); sendState("Tăng nhiệt độ") }
        ))
        controlPanel.addView(horizontalRow(
            actionButton("ĐỔI CHẾ ĐỘ") { val modes = LgAcProtocol.Mode.entries; state = state.copy(mode = modes[(state.mode.ordinal + 1) % modes.size], power = true); sendState("Chế độ") },
            actionButton("TỐC ĐỘ QUẠT") { val fans = listOf(LgAcProtocol.Fan.AUTO,LgAcProtocol.Fan.LOWEST,LgAcProtocol.Fan.LOW,LgAcProtocol.Fan.MEDIUM,LgAcProtocol.Fan.MAX); val current = fans.indexOf(state.fan).takeIf { it >= 0 } ?: 0; state = state.copy(fan = fans[(current + 1) % fans.size], power = true); sendState("Quạt") }
        ))
        controlPanel.addView(horizontalRow(
            actionButton("ĐẢO GIÓ") { sendSwing() },
            actionButton("GỬI LẠI") { sendState("Gửi lại") }
        ))
        root.addView(controlPanel)

        val timerPanel = section("HẸN GIỜ THÔNG MINH")
        timerStatus = label(16f).apply { gravity = Gravity.CENTER }
        timerPanel.addView(timerStatus)
        timerPanel.addView(horizontalRow(
            actionButton("HẸN BẬT") { openTimerDialog(true) },
            actionButton("HẸN TẮT") { openTimerDialog(false) }
        ))
        timerPanel.addView(horizontalRow(
            actionButton("TẮT SAU 30 PHÚT") { scheduleAfter(false, 30) },
            actionButton("TẮT SAU 1 GIỜ") { scheduleAfter(false, 60) }
        ))
        timerPanel.addView(actionButton("HỦY LỊCH HẸN") { TimerScheduler.cancel(this); toast("Đã hủy lịch hẹn"); refreshUi() })
        timerPanel.addView(infoText("Lưu ý: khi đến giờ, điện thoại phải còn pin và đầu phát hồng ngoại cần hướng về phía điều hòa."))
        root.addView(timerPanel)

        val settings = section("CẤU HÌNH")
        settings.addView(actionButton("XÓA MÃ ĐÃ LƯU VÀ DÒ LẠI") { resetProfile() })
        settings.addView(infoText("Ứng dụng hoạt động ngoại tuyến, không quảng cáo và giữ nguyên cơ chế dò mã LG/LG2 của V1.0."))
        root.addView(settings)
        return ScrollView(this).apply { addView(root) }
    }

    private fun section(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = panelDrawable()
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(9), 0, dp(9)) }
        addView(TextView(this@MainActivity).apply {
            text = title; textSize = 14f; letterSpacing = .12f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(90, 220, 255)); setPadding(0, 0, 0, dp(10))
        })
    }

    private fun panelDrawable() = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(18).toFloat()
        setColor(Color.rgb(9, 20, 39))
        setStroke(dp(1), Color.rgb(35, 105, 145))
    }

    private fun buttonDrawable(prominent: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(14).toFloat()
        setColor(if (prominent) Color.rgb(10, 78, 112) else Color.rgb(14, 34, 58))
        setStroke(dp(1), if (prominent) Color.rgb(80, 220, 255) else Color.rgb(40, 100, 135))
    }

    private fun label(size: Float) = TextView(this).apply { textSize = size; setTextColor(Color.rgb(220, 235, 248)); setPadding(0, dp(4), 0, dp(8)) }
    private fun infoText(value: String) = TextView(this).apply { text = value; textSize = 13f; setTextColor(Color.rgb(135, 160, 184)); setPadding(0, dp(5), 0, dp(10)) }
    private fun actionButton(textValue: String, prominent: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = textValue; textSize = if (prominent) 15f else 14f; isAllCaps = false; minimumHeight = dp(56)
        setTextColor(Color.WHITE); background = buttonDrawable(prominent); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(5), dp(3), dp(5)) }
    }
    private fun horizontalRow(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        views.forEach { view -> view.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) }; addView(view) }
    }

    private fun openTimerDialog(turnOn: Boolean) {
        if (selectedProfile == null) return toast("Hãy dò và lưu mã trước.")
        val now = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
        TimePickerDialog(this, { _, hour, minute ->
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            scheduleAt(turnOn, target.timeInMillis)
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
    }

    private fun scheduleAfter(turnOn: Boolean, minutes: Int) = scheduleAt(turnOn, System.currentTimeMillis() + minutes * 60_000L)

    private fun scheduleAt(turnOn: Boolean, triggerAt: Long) {
        val profile = selectedProfile ?: return toast("Hãy dò và lưu mã trước.")
        val scheduledState = if (turnOn) state.copy(power = true) else state.copy(power = false)
        TimerScheduler.schedule(this, triggerAt, profile, scheduledState)
        toast("Đã hẹn ${if (turnOn) "bật" else "tắt"} điều hòa")
        refreshUi()
    }

    private fun changeCandidate(delta: Int) { val size = LgAcProtocol.scanCandidates.size; candidateIndex = (candidateIndex + delta + size) % size; prefs.edit().putInt("candidate_index", candidateIndex).apply(); refreshUi() }
    private fun transmitCurrentCandidate() { val c = LgAcProtocol.scanCandidates[candidateIndex]; transmit(c.profile, LgAcProtocol.encodeState(c.state), "Đã phát mã thử") }
    private fun saveCurrentCandidate() {
        val c = LgAcProtocol.scanCandidates[candidateIndex]; selectedProfile = c.profile; state = c.state
        prefs.edit().putString("profile_id", c.profile.id).putBoolean("power", state.power).putInt("temperature", state.temperature).putString("mode", state.mode.name).putString("fan", state.fan.name).putBoolean("swing", state.swing).apply()
        toast("Đã lưu: ${c.profile.displayName}"); refreshUi()
    }
    private fun sendState(reason: String) { val p = selectedProfile ?: return toast("Hãy dò và lưu mã trước."); if (transmit(p, LgAcProtocol.encodeState(state), reason)) saveControlState() }
    private fun sendSwing() {
        val p = selectedProfile ?: return toast("Hãy dò và lưu mã trước."); state = state.copy(swing = !state.swing)
        if (transmit(p, LgAcProtocol.swingCommand(p, state.swing), if (state.swing) "Bật đảo gió" else "Tắt đảo gió")) saveControlState() else state = state.copy(swing = !state.swing)
    }
    private fun transmit(profile: LgAcProtocol.Profile, raw: Int, reason: String): Boolean = ir.send(profile, raw).fold(
        onSuccess = { toast("$reason – 0x${raw.toString(16).uppercase()}"); refreshUi(); true },
        onFailure = { toast("Không phát được IR: ${it.message}"); false }
    )
    private fun saveControlState() { prefs.edit().putBoolean("power", state.power).putInt("temperature", state.temperature).putString("mode", state.mode.name).putString("fan", state.fan.name).putBoolean("swing", state.swing).apply(); refreshUi() }
    private fun resetProfile() { TimerScheduler.cancel(this); selectedProfile = null; state = LgAcProtocol.State(); prefs.edit().remove("profile_id").apply(); toast("Đã xóa mã. Hãy dò lại."); refreshUi() }

    private fun refreshUi() {
        irStatus.text = if (ir.isAvailable) "● IR BLASTER: SẴN SÀNG" else "● KHÔNG PHÁT HIỆN IR BLASTER"
        irStatus.setTextColor(if (ir.isAvailable) Color.rgb(85, 240, 170) else Color.rgb(255, 95, 110))
        val c = LgAcProtocol.scanCandidates[candidateIndex]
        scanStatus.text = "MÃ ${candidateIndex + 1}/${LgAcProtocol.scanCandidates.size}\n${c.description}"
        val p = selectedProfile
        scanPanel.visibility = if (p == null) View.VISIBLE else View.GONE
        controlPanel.visibility = if (p != null) View.VISIBLE else View.GONE
        if (p != null) {
            val raw = LgAcProtocol.encodeState(state)
            stateStatus.text = "${state.temperature}°C\n${if (state.power) "ĐANG BẬT" else "ĐANG TẮT"}  •  ${state.mode.label}\nQUẠT ${state.fan.label.uppercase()}  •  ĐẢO GIÓ ${if (state.swing) "BẬT" else "TẮT"}\n${p.displayName}\nIR 0x${raw.toString(16).uppercase()}"
            powerButton.text = if (state.power) "TẮT ĐIỀU HÒA" else "BẬT ĐIỀU HÒA"
        }
        val active = prefs.getBoolean(TimerScheduler.KEY_ACTIVE, false)
        val at = prefs.getLong(TimerScheduler.KEY_TIME, 0L)
        val turnOn = prefs.getBoolean(TimerScheduler.KEY_POWER, false)
        timerStatus.text = if (active && at > System.currentTimeMillis()) {
            val format = SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.getDefault())
            "LỊCH ĐANG CHẠY\n${if (turnOn) "BẬT" else "TẮT"} lúc ${format.format(Date(at))}"
        } else "CHƯA CÓ LỊCH HẸN"
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
