package com.minh.autotouch

import android.content.Context
import org.json.JSONObject

object ConfigStore {
    private const val PREFS = "auto_touch_prefs"
    private const val KEY_CONFIG = "config_json"

    @Synchronized
    fun load(context: Context): AppConfig {
        val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CONFIG, null)
        return if (text.isNullOrBlank()) AppConfig() else runCatching { AppConfig.fromJson(JSONObject(text)) }.getOrElse { AppConfig() }
    }

    @Synchronized
    fun save(context: Context, config: AppConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CONFIG, config.toJson().toString()).apply()
    }

    fun exportText(context: Context): String = load(context).toJson().toString(2)

    fun importText(context: Context, text: String): AppConfig {
        val config = AppConfig.fromJson(JSONObject(text))
        save(context, config)
        return config
    }
}
