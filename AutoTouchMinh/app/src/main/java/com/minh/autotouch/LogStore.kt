package com.minh.autotouch

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogStore {
    private const val FILE_NAME = "automation_log.csv"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Synchronized
    fun append(context: Context, event: String, detail: String = "") {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) file.writeText("time,event,detail\n")
        val safeEvent = event.replace("\"", "\"\"")
        val safeDetail = detail.replace("\"", "\"\"")
        file.appendText("\"${dateFormat.format(Date())}\",\"$safeEvent\",\"$safeDetail\"\n")
    }

    fun read(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else "time,event,detail\n"
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}
