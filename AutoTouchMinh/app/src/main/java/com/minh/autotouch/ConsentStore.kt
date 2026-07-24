package com.minh.autotouch

import android.content.Context

object ConsentStore {
    private const val PREFS = "autotouch_consent"
    private const val KEY_ACCEPTED = "accessibility_disclosure_accepted_v1"

    fun isAccepted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACCEPTED, false)

    fun setAccepted(context: Context, accepted: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACCEPTED, accepted)
            .apply()
    }
}
