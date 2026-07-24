package com.minh.autotouch

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.minh.autotouch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.edtLoops.setText(ConfigStore.getLoops(this).toString())
        binding.edtBetweenCycles.setText(ConfigStore.getBetweenCycles(this).toString())

        binding.btnOverlayPermission.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnStartOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Hãy cấp quyền hiển thị nổi trước.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
            ContextCompat.startForegroundService(
                this,
                Intent(this, FloatingControlService::class.java)
            )
        }

        binding.btnSave.setOnClickListener {
            ConfigStore.setLoops(
                this,
                binding.edtLoops.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
            )
            ConfigStore.setBetweenCycles(
                this,
                binding.edtBetweenCycles.text.toString().toLongOrNull()?.coerceAtLeast(0) ?: 500L
            )
            Toast.makeText(this, "Đã lưu cấu hình chung.", Toast.LENGTH_SHORT).show()
        }
    }
}
