package com.minh.autotouch.installtest

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(TextView(this).apply {
            text = "AUTO TOUCH MINH\nINSTALLATION TEST"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(13, 71, 161))
        })
        root.addView(TextView(this).apply {
            text = "Cài đặt và mở ứng dụng thành công.\n\nBản này không có Trợ năng, cửa sổ nổi, dịch vụ nền hoặc quyền đặc biệt."
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 32, 0, 0)
        })
        setContentView(root)
    }
}
