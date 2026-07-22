package vn.minh.lgirremote

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import vn.minh.lgirremote.ir.IrTransmitter
import vn.minh.lgirremote.ir.LgAcProtocol

class MainActivity : Activity() {
    private lateinit var ir: IrTransmitter
    private val prefs by lazy { getSharedPreferences("lg_ir_remote", MODE_PRIVATE) }
    private lateinit var irStatus: TextView
    private lateinit var scanStatus: TextView
    private lateinit var stateStatus: TextView
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

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T = runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)

    private fun buildUi(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(28)) }
        root.addView(TextView(this).apply { text = "Điều khiển điều hòa LG"; textSize = 26f; setTextColor(Color.rgb(13,71,161)); gravity = Gravity.CENTER; setPadding(0,0,0,dp(8)) })
        irStatus = label(16f); root.addView(irStatus)

        scanPanel = section("1. Dò mã điều hòa")
        scanStatus = label(17f); scanPanel.addView(scanStatus)
        scanPanel.addView(infoText("Hướng đầu trên Redmi 10 về phía mắt nhận điều hòa, cách khoảng 1–3 m. Nhấn PHÁT MÃ THỬ; nếu điều hòa bật hoặc kêu bíp, hãy lưu mã."))
        scanPanel.addView(actionButton("PHÁT MÃ THỬ", true) { transmitCurrentCandidate() })
        scanPanel.addView(horizontalRow(actionButton("← Mã trước") { changeCandidate(-1) }, actionButton("Mã tiếp →") { changeCandidate(1) }))
        scanPanel.addView(actionButton("✓ MÃ NÀY HOẠT ĐỘNG – LƯU", true) { saveCurrentCandidate() })
        root.addView(scanPanel)

        controlPanel = section("2. Điều khiển")
        stateStatus = label(19f).apply { gravity = Gravity.CENTER }; controlPanel.addView(stateStatus)
        powerButton = actionButton("BẬT", true) { state = state.copy(power = !state.power); sendState("Nguồn") }; controlPanel.addView(powerButton)
        controlPanel.addView(horizontalRow(
            actionButton("− Nhiệt độ", true) { state = state.copy(temperature = (state.temperature - 1).coerceAtLeast(16), power = true); sendState("Giảm nhiệt độ") },
            actionButton("+ Nhiệt độ", true) { state = state.copy(temperature = (state.temperature + 1).coerceAtMost(30), power = true); sendState("Tăng nhiệt độ") }
        ))
        controlPanel.addView(horizontalRow(
            actionButton("Đổi chế độ") { val modes = LgAcProtocol.Mode.entries; state = state.copy(mode = modes[(state.mode.ordinal + 1) % modes.size], power = true); sendState("Chế độ") },
            actionButton("Đổi tốc độ quạt") { val fans = listOf(LgAcProtocol.Fan.AUTO,LgAcProtocol.Fan.LOWEST,LgAcProtocol.Fan.LOW,LgAcProtocol.Fan.MEDIUM,LgAcProtocol.Fan.MAX); val current = fans.indexOf(state.fan).takeIf { it >= 0 } ?: 0; state = state.copy(fan = fans[(current + 1) % fans.size], power = true); sendState("Quạt") }
        ))
        controlPanel.addView(actionButton("ĐẢO GIÓ", true) { sendSwing() })
        controlPanel.addView(actionButton("GỬI LẠI TRẠNG THÁI HIỆN TẠI") { sendState("Gửi lại") })
        controlPanel.addView(actionButton("XÓA MÃ ĐÃ LƯU VÀ DÒ LẠI") { resetProfile() })
        root.addView(controlPanel)
        root.addView(infoText("Ứng dụng hoạt động ngoại tuyến. Một số điều hòa LG cửa sổ/di động có thể dùng giao thức khác và cần bổ sung mã từ remote gốc."))
        return ScrollView(this).apply { addView(root) }
    }

    private fun section(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(Color.rgb(245,248,252))
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(10),0,dp(10)) }
        addView(TextView(this@MainActivity).apply { text = title; textSize = 20f; setTextColor(Color.rgb(30,30,30)); setPadding(0,0,0,dp(8)) })
    }
    private fun label(size: Float) = TextView(this).apply { textSize = size; setTextColor(Color.DKGRAY); setPadding(0,dp(4),0,dp(8)) }
    private fun infoText(value: String) = TextView(this).apply { text = value; textSize = 14f; setTextColor(Color.GRAY); setPadding(0,dp(4),0,dp(10)) }
    private fun actionButton(textValue: String, prominent: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = textValue; textSize = if (prominent) 17f else 15f; isAllCaps = false; minimumHeight = dp(54); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(dp(3),dp(4),dp(3),dp(4)) }
    }
    private fun horizontalRow(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        views.forEach { view -> view.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(dp(3),dp(4),dp(3),dp(4)) }; addView(view) }
    }

    private fun changeCandidate(delta: Int) { val size = LgAcProtocol.scanCandidates.size; candidateIndex = (candidateIndex + delta + size) % size; prefs.edit().putInt("candidate_index", candidateIndex).apply(); refreshUi() }
    private fun transmitCurrentCandidate() { val c = LgAcProtocol.scanCandidates[candidateIndex]; transmit(c.profile, LgAcProtocol.encodeState(c.state), "Đã phát mã thử") }
    private fun saveCurrentCandidate() {
        val c = LgAcProtocol.scanCandidates[candidateIndex]; selectedProfile = c.profile; state = c.state
        prefs.edit().putString("profile_id",c.profile.id).putBoolean("power",state.power).putInt("temperature",state.temperature).putString("mode",state.mode.name).putString("fan",state.fan.name).putBoolean("swing",state.swing).apply()
        toast("Đã lưu: ${c.profile.displayName}"); refreshUi()
    }
    private fun sendState(reason: String) { val p = selectedProfile ?: return toast("Hãy dò và lưu mã trước."); if (transmit(p,LgAcProtocol.encodeState(state),reason)) saveControlState() }
    private fun sendSwing() {
        val p = selectedProfile ?: return toast("Hãy dò và lưu mã trước."); state = state.copy(swing = !state.swing)
        if (transmit(p,LgAcProtocol.swingCommand(p,state.swing),if(state.swing)"Bật đảo gió" else "Tắt đảo gió")) saveControlState() else state = state.copy(swing = !state.swing)
    }
    private fun transmit(profile: LgAcProtocol.Profile, raw: Int, reason: String): Boolean = ir.send(profile,raw).fold(
        onSuccess = { toast("$reason – 0x${raw.toString(16).uppercase()}"); refreshUi(); true },
        onFailure = { toast("Không phát được IR: ${it.message}"); false }
    )
    private fun saveControlState() { prefs.edit().putBoolean("power",state.power).putInt("temperature",state.temperature).putString("mode",state.mode.name).putString("fan",state.fan.name).putBoolean("swing",state.swing).apply(); refreshUi() }
    private fun resetProfile() { selectedProfile = null; state = LgAcProtocol.State(); prefs.edit().remove("profile_id").apply(); toast("Đã xóa mã. Hãy dò lại."); refreshUi() }

    private fun refreshUi() {
        irStatus.text = if (ir.isAvailable) "✓ Redmi 10 báo có bộ phát hồng ngoại." else "⚠ Android không phát hiện IR Blaster."
        irStatus.setTextColor(if(ir.isAvailable) Color.rgb(27,94,32) else Color.RED)
        val c = LgAcProtocol.scanCandidates[candidateIndex]; scanStatus.text = "Mã ${candidateIndex + 1}/${LgAcProtocol.scanCandidates.size}\n${c.description}"
        val p = selectedProfile; scanPanel.visibility = if(p==null) View.VISIBLE else View.GONE; controlPanel.visibility = if(p!=null) View.VISIBLE else View.GONE
        if (p != null) {
            val raw = LgAcProtocol.encodeState(state)
            stateStatus.text = "${if(state.power)"ĐANG BẬT" else "ĐANG TẮT"}  •  ${state.temperature}°C\n${state.mode.label}  •  Quạt ${state.fan.label}  •  Đảo gió ${if(state.swing)"bật" else "tắt"}\nMã đã lưu: ${p.displayName}\nIR: 0x${raw.toString(16).uppercase()}"
            powerButton.text = if(state.power) "TẮT ĐIỀU HÒA" else "BẬT ĐIỀU HÒA"
        }
    }
    private fun toast(message: String) = Toast.makeText(this,message,Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
