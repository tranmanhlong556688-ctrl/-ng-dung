package vn.mltudonghoa.cuncung

import android.content.Context

object Prefs {
    const val QUALITY_SAVER = "saver"
    const val QUALITY_BALANCED = "balanced"
    const val QUALITY_BEST = "best"

    const val MERGE_AUDIO_RECORDING = "recording"
    const val MERGE_AUDIO_SELECTED = "selected"
    const val MERGE_AUDIO_NONE = "none"

    data class MarkerPoint(val x: Float, val y: Float)

    private const val NAME = "ml_cun_cung"
    private const val KEY_AUTO_MOVE = "auto_move"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_AUDIO_URI = "audio_uri"
    private const val KEY_LAST_RECORDING = "last_recording"
    private const val KEY_QUALITY = "quality"
    private const val KEY_TWIN_ENABLED = "twin_enabled"
    private const val KEY_TWIN_INTERVAL_MIN = "twin_interval_min"
    private const val KEY_MERGE_AUDIO = "merge_audio"
    private const val KEY_MARKERS = "icon_markers"
    private const val KEY_PAUSE_LOW_BATTERY = "pause_low_battery"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"
    private const val KEY_FEEDBACK_ENABLED = "feedback_enabled"
    private const val KEY_FEEDBACK_STAGE = "feedback_stage"
    private const val KEY_FEEDBACK_DUE = "feedback_due"

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun autoMove(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_MOVE, true)
    fun setAutoMove(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_MOVE, enabled).apply()

    fun autoStart(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_START, false)
    fun setAutoStart(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_START, enabled).apply()

    fun audioUri(context: Context): String? = prefs(context).getString(KEY_AUDIO_URI, null)
    fun setAudioUri(context: Context, uri: String) = prefs(context).edit().putString(KEY_AUDIO_URI, uri).apply()

    fun recordingPath(context: Context): String? = prefs(context).getString(KEY_LAST_RECORDING, null)
    fun setRecordingPath(context: Context, path: String) = prefs(context).edit().putString(KEY_LAST_RECORDING, path).apply()

    fun quality(context: Context): String = prefs(context).getString(KEY_QUALITY, QUALITY_BALANCED) ?: QUALITY_BALANCED
    fun setQuality(context: Context, value: String) = prefs(context).edit().putString(KEY_QUALITY, value).apply()

    fun twinEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_TWIN_ENABLED, true)
    fun setTwinEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_TWIN_ENABLED, enabled).apply()

    fun twinIntervalMinutes(context: Context): Int = prefs(context).getInt(KEY_TWIN_INTERVAL_MIN, 5).coerceIn(1, 60)
    fun setTwinIntervalMinutes(context: Context, minutes: Int) = prefs(context).edit().putInt(KEY_TWIN_INTERVAL_MIN, minutes.coerceIn(1, 60)).apply()

    fun mergeAudio(context: Context): String = prefs(context).getString(KEY_MERGE_AUDIO, MERGE_AUDIO_RECORDING) ?: MERGE_AUDIO_RECORDING
    fun setMergeAudio(context: Context, value: String) = prefs(context).edit().putString(KEY_MERGE_AUDIO, value).apply()

    fun pauseLowBattery(context: Context): Boolean = prefs(context).getBoolean(KEY_PAUSE_LOW_BATTERY, true)
    fun setPauseLowBattery(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_PAUSE_LOW_BATTERY, enabled).apply()

    fun quietStart(context: Context): Int = prefs(context).getInt(KEY_QUIET_START, 22).coerceIn(0, 23)
    fun quietEnd(context: Context): Int = prefs(context).getInt(KEY_QUIET_END, 7).coerceIn(0, 23)
    fun setQuietHours(context: Context, start: Int, end: Int) = prefs(context).edit()
        .putInt(KEY_QUIET_START, start.coerceIn(0, 23))
        .putInt(KEY_QUIET_END, end.coerceIn(0, 23)).apply()

    fun isQuietNow(context: Context, hour: Int): Boolean {
        val start = quietStart(context)
        val end = quietEnd(context)
        return if (start == end) false else if (start < end) hour in start until end else hour >= start || hour < end
    }

    fun markers(context: Context): List<MarkerPoint> {
        val raw = prefs(context).getString(KEY_MARKERS, "").orEmpty()
        return raw.split(';').mapNotNull { item ->
            val parts = item.split(',')
            val x = parts.getOrNull(0)?.toFloatOrNull()
            val y = parts.getOrNull(1)?.toFloatOrNull()
            if (x != null && y != null) MarkerPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)) else null
        }
    }

    fun setMarkers(context: Context, points: List<MarkerPoint>) {
        val raw = points.joinToString(";") { "${it.x},${it.y}" }
        prefs(context).edit().putString(KEY_MARKERS, raw).apply()
    }

    fun feedbackEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_FEEDBACK_ENABLED, true)
    fun setFeedbackEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_FEEDBACK_ENABLED, enabled).apply()

    fun feedbackStage(context: Context): Int = prefs(context).getInt(KEY_FEEDBACK_STAGE, 0)
    fun feedbackDue(context: Context): Long = prefs(context).getLong(KEY_FEEDBACK_DUE, 0L)
    fun setFeedbackSchedule(context: Context, stage: Int, due: Long) = prefs(context).edit()
        .putInt(KEY_FEEDBACK_STAGE, stage).putLong(KEY_FEEDBACK_DUE, due).apply()
}
