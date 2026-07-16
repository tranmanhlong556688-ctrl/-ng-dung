package vn.mltudonghoa.cuncung

import android.content.Context

object Prefs {
    private const val NAME = "ml_cun_cung"
    private const val KEY_AUTO_MOVE = "auto_move"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_AUDIO_URI = "audio_uri"
    private const val KEY_LAST_RECORDING = "last_recording"

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun autoMove(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_MOVE, true)
    fun setAutoMove(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_MOVE, enabled).apply()

    fun autoStart(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_START, false)
    fun setAutoStart(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_START, enabled).apply()

    fun audioUri(context: Context): String? = prefs(context).getString(KEY_AUDIO_URI, null)
    fun setAudioUri(context: Context, uri: String) = prefs(context).edit().putString(KEY_AUDIO_URI, uri).apply()

    fun recordingPath(context: Context): String? = prefs(context).getString(KEY_LAST_RECORDING, null)
    fun setRecordingPath(context: Context, path: String) = prefs(context).edit().putString(KEY_LAST_RECORDING, path).apply()
}
