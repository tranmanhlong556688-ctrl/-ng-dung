package com.minh.autokiemtrabinh

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun AutoAccessibilityService.ensureOverlays() {
    if (controllerView == null) createController()
    if (miniView == null) createMiniController()
    if (p1View == null) createMarker(PointId.P1)
    if (p2View == null) createMarker(PointId.P2)
    updateCoordinates()
}

private fun AutoAccessibilityService.createController() {
    val view = LayoutInflater.from(this).inflate(R.layout.overlay_controller, null)
    val width = (resources.displayMetrics.widthPixels - dp(16)).coerceAtMost(dp(370))
    val params = WindowManager.LayoutParams(
        width,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = Prefs.controllerX(this@createController)
        y = Prefs.controllerY(this@createController)
    }

    statusText = view.findViewById(R.id.tvStatus)
    counterText = view.findViewById(R.id.tvCounter)
    coordinateText = view.findViewById(R.id.tvCoordinates)
    roundsInput = view.findViewById<EditText>(R.id.etRounds).apply { setText(Prefs.rounds(this@createController).toString()) }
    infiniteCheck = view.findViewById<CheckBox>(R.id.cbInfinite).apply { isChecked = Prefs.infinite(this@createController) }
    betweenP1Input = view.findViewById<EditText>(R.id.etBetweenP1).apply { setText(Prefs.betweenP1(this@createController).toString()) }
    beforeP2Input = view.findViewById<EditText>(R.id.etBeforeP2).apply { setText(Prefs.beforeP2(this@createController).toString()) }
    afterP2Input = view.findViewById<EditText>(R.id.etAfterP2).apply { setText(Prefs.afterP2(this@createController).toString()) }
    tapDurationInput = view.findViewById<EditText>(R.id.etTapDuration).apply { setText(Prefs.tapDuration(this@createController).toString()) }
    pauseResumeButton = view.findViewById(R.id.btnPauseResume)
    setupButton = view.findViewById(R.id.btnSetup)

    infiniteCheck?.setOnCheckedChangeListener { _, checked -> roundsInput?.isEnabled = !checked }
    roundsInput?.isEnabled = infiniteCheck?.isChecked != true

    listOf(roundsInput, betweenP1Input, beforeP2Input, afterP2Input, tapDurationInput).forEach { edit ->
        edit?.setOnFocusChangeListener { focusedView, hasFocus ->
            if (hasFocus) handler.postDelayed({
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(focusedView, InputMethodManager.SHOW_IMPLICIT)
            }, 120L)
        }
    }

    view.findViewById<View>(R.id.dragHandle).setOnTouchListener(makeDragListener(params, PointId.P1, isController = true))
    view.findViewById<Button>(R.id.btnMinimize).setOnClickListener { showController(false) }
    view.findViewById<Button>(R.id.btnSetup).setOnClickListener { toggleSetupMode() }
    view.findViewById<Button>(R.id.btnTestP1).setOnClickListener { testPoint(PointId.P1) }
    view.findViewById<Button>(R.id.btnTestP2).setOnClickListener { testPoint(PointId.P2) }
    view.findViewById<Button>(R.id.btnStart).setOnClickListener { startAutomation() }
    pauseResumeButton?.setOnClickListener { togglePauseResume() }
    view.findViewById<Button>(R.id.btnStop).setOnClickListener { stopAutomation("Đã dừng khẩn cấp", true) }
    view.findViewById<Button>(R.id.btnResetTime).setOnClickListener {
        betweenP1Input?.setText("150")
        beforeP2Input?.setText("300")
        afterP2Input?.setText("400")
        tapDurationInput?.setText("60")
        Toast.makeText(this, "Đã khôi phục thời gian mặc định.", Toast.LENGTH_SHORT).show()
    }
    view.findViewById<Button>(R.id.btnClearPoints).setOnClickListener {
        if (state == RunState.RUNNING || state == RunState.COUNTDOWN) {
            Toast.makeText(this, "Hãy dừng chương trình trước.", Toast.LENGTH_SHORT).show()
        } else {
            Prefs.clearPoints(this)
            placeMarkersAtDefaults()
            setSetupMode(true)
            updateCoordinates()
            Toast.makeText(this, "Đã xóa tọa độ cũ.", Toast.LENGTH_SHORT).show()
        }
    }

    windowManager.addView(view, params)
    controllerView = view
    controllerParams = params
}

private fun AutoAccessibilityService.createMiniController() {
    val view = LayoutInflater.from(this).inflate(R.layout.overlay_minimized, null)
    val params = WindowManager.LayoutParams(
        dp(62), dp(62),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = Prefs.miniX(this@createMiniController)
        y = Prefs.miniY(this@createMiniController)
    }
    view.setOnTouchListener(makeDragListener(params, PointId.P1, isMini = true))
    view.setOnClickListener { showController(true) }
    windowManager.addView(view, params)
    miniView = view
    miniParams = params
}

private fun AutoAccessibilityService.createMarker(point: PointId) {
    val view = LayoutInflater.from(this).inflate(R.layout.overlay_marker, null)
    val markerText = view.findViewById<TextView>(R.id.tvMarker)
    markerText.text = if (point == PointId.P1) "P1" else "P2"
    markerText.setBackgroundResource(if (point == PointId.P1) R.drawable.bg_p1 else R.drawable.bg_p2)

    val size = dp(56)
    val savedX = if (point == PointId.P1) Prefs.p1X(this) else Prefs.p2X(this)
    val savedY = if (point == PointId.P1) Prefs.p1Y(this) else Prefs.p2Y(this)
    val defaultCenterX = resources.displayMetrics.widthPixels * if (point == PointId.P1) 0.30f else 0.70f
    val defaultCenterY = resources.displayMetrics.heightPixels * 0.78f
    val params = WindowManager.LayoutParams(
        size, size,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = ((if (savedX > 0f) savedX else defaultCenterX) - size / 2f).roundToInt()
        y = ((if (savedY > 0f) savedY else defaultCenterY) - size / 2f).roundToInt()
    }
    view.setOnTouchListener(makeDragListener(params, point))
    windowManager.addView(view, params)
    view.visibility = View.GONE
    if (point == PointId.P1) {
        p1View = view
        p1Params = params
    } else {
        p2View = view
        p2Params = params
    }
}

private fun AutoAccessibilityService.placeMarkersAtDefaults() {
    val size = dp(56)
    p1Params?.apply {
        x = (resources.displayMetrics.widthPixels * 0.30f - size / 2f).roundToInt()
        y = (resources.displayMetrics.heightPixels * 0.78f - size / 2f).roundToInt()
        p1View?.let { windowManager.updateViewLayout(it, this) }
    }
    p2Params?.apply {
        x = (resources.displayMetrics.widthPixels * 0.70f - size / 2f).roundToInt()
        y = (resources.displayMetrics.heightPixels * 0.78f - size / 2f).roundToInt()
        p2View?.let { windowManager.updateViewLayout(it, this) }
    }
}

private fun AutoAccessibilityService.makeDragListener(
    params: WindowManager.LayoutParams,
    point: PointId,
    isController: Boolean = false,
    isMini: Boolean = false
): View.OnTouchListener {
    var downRawX = 0f
    var downRawY = 0f
    var startX = 0
    var startY = 0
    var moved = false
    return View.OnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX; downRawY = event.rawY
                startX = params.x; startY = params.y; moved = false; true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downRawX).roundToInt()
                val dy = (event.rawY - downRawY).roundToInt()
                if (abs(dx) + abs(dy) > dp(5)) moved = true
                val maxX = (resources.displayMetrics.widthPixels - view.width.coerceAtLeast(dp(56))).coerceAtLeast(0)
                val maxY = (resources.displayMetrics.heightPixels - view.height.coerceAtLeast(dp(56))).coerceAtLeast(0)
                params.x = (startX + dx).coerceIn(0, maxX)
                params.y = (startY + dy).coerceIn(0, maxY)
                windowManager.updateViewLayout(view, params)
                if (!isController && !isMini) updateCoordinatesFromParams()
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                when {
                    isController -> Prefs.saveControllerPosition(this, params.x, params.y)
                    isMini -> {
                        Prefs.saveMiniPosition(this, params.x, params.y)
                        if (!moved && event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                    }
                    else -> {
                        val centerX = params.x + view.width / 2f
                        val centerY = params.y + view.height / 2f
                        if (point == PointId.P1) Prefs.saveP1(this, centerX, centerY)
                        else Prefs.saveP2(this, centerX, centerY)
                        updateCoordinates()
                    }
                }
                true
            }
            else -> false
        }
    }
}

internal fun AutoAccessibilityService.showController(showFull: Boolean) {
    ensureOverlays()
    controllerView?.visibility = if (showFull) View.VISIBLE else View.GONE
    miniView?.visibility = if (showFull) View.GONE else View.VISIBLE
    Prefs.saveCollapsed(this, !showFull)
}

internal fun AutoAccessibilityService.toggleSetupMode() {
    if (state == RunState.RUNNING || state == RunState.COUNTDOWN) {
        Toast.makeText(this, "Hãy dừng hoặc tạm dừng trước khi chỉnh P1/P2.", Toast.LENGTH_SHORT).show()
    } else setSetupMode(!setupMode)
}

internal fun AutoAccessibilityService.setSetupMode(enabled: Boolean) {
    setupMode = enabled
    p1View?.visibility = if (enabled) View.VISIBLE else View.GONE
    p2View?.visibility = if (enabled) View.VISIBLE else View.GONE
    setupButton?.text = if (enabled) "KHÓA P1/P2" else "THIẾT LẬP"
    updateCoordinates()
}

private fun AutoAccessibilityService.updateCoordinatesFromParams() {
    val p1x = p1Params?.let { it.x + dp(28) } ?: 0
    val p1y = p1Params?.let { it.y + dp(28) } ?: 0
    val p2x = p2Params?.let { it.x + dp(28) } ?: 0
    val p2y = p2Params?.let { it.y + dp(28) } ?: 0
    coordinateText?.text = "P1: $p1x, $p1y | P2: $p2x, $p2y"
}

internal fun AutoAccessibilityService.updateCoordinates() {
    val p1 = if (Prefs.isP1Set(this)) "${Prefs.p1X(this).roundToInt()}, ${Prefs.p1Y(this).roundToInt()}" else "chưa đặt"
    val p2 = if (Prefs.isP2Set(this)) "${Prefs.p2X(this).roundToInt()}, ${Prefs.p2Y(this).roundToInt()}" else "chưa đặt"
    coordinateText?.text = "P1: $p1 | P2: $p2"
}

internal fun AutoAccessibilityService.updateCounter() {
    counterText?.text = if (runInfinite) "Đã hoàn thành: $completedRounds vòng | Không giới hạn"
    else "Đã hoàn thành: $completedRounds / $requestedRounds vòng"
}

internal fun AutoAccessibilityService.updateUi(message: String) {
    statusText?.text = "Trạng thái: $message"
    statusText?.setTextColor(when (state) {
        RunState.RUNNING, RunState.COMPLETED -> Color.rgb(27, 94, 32)
        RunState.PAUSED, RunState.COUNTDOWN -> Color.rgb(230, 126, 34)
        RunState.ERROR, RunState.IDLE -> Color.rgb(198, 40, 40)
    })
    pauseResumeButton?.text = if (state == RunState.PAUSED) "TIẾP TỤC" else "TẠM DỪNG"
    updateCounter()
    notifyState()
}

internal fun AutoAccessibilityService.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()
