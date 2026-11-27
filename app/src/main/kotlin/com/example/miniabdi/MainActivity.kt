package com.example.miniabdi

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val title = TextView(this).apply {
            text = "MINI ABDI"
            textSize = 28f
        }

        val desc = TextView(this).apply {
            text = "همراه شناور روی صفحه با سوال‌های ریاضی هر ۱ دقیقه\n\nبرای حذف کامل آدمک: آن را به پایین صفحه بکشید."
            textSize = 16f
            setPadding(0, 24, 0, 40)
        }

        val btnStart = Button(this).apply {
            text = "فعال کردن آدمک شناور"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "ابتدا مجوز نمایش روی سایر برنامه‌ها را بدهید", Toast.LENGTH_LONG).show()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    val serviceIntent = Intent(this@MainActivity, FloatingService::class.java)
                    ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                    Toast.makeText(this@MainActivity, "MINI ABDI فعال شد!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnStop = Button(this).apply {
            text = "خاموش کردن کامل"
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatingService::class.java))
                Toast.makeText(this@MainActivity, "آدمک بسته شد", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(title)
        layout.addView(desc)
        layout.addView(btnStart)
        layout.addView(btnStop)
        setContentView(layout)
    }
}
