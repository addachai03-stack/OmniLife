package com.tapstorm.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("tapstorm", MODE_PRIVATE) }
    private lateinit var packageInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(42, 54, 42, 36)
        }
        layout.addView(TextView(this).apply {
            text = "TAPSTORM"
            textSize = 28f
        })
        layout.addView(TextView(this).apply {
            text = "Refresh-only foreground assistant"
            textSize = 16f
        })
        packageInput = EditText(this).apply {
            hint = "Target app package name"
            singleLine = true
            inputType = InputType.TYPE_CLASS_TEXT
            setText(prefs.getString("target_package", ""))
        }
        layout.addView(packageInput)
        intervalInput = EditText(this).apply {
            hint = "Interval in seconds (10–300)"
            singleLine = true
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getLong("interval_seconds", 30L).toString())
        }
        layout.addView(intervalInput)
        status = TextView(this).apply {
            text = "Allowed action: exact visible button text “Refresh” or “Reload”."
            textSize = 14f
            setPadding(0, 24, 0, 24)
        }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "Save settings"
            setOnClickListener { saveSettings() }
        })
        layout.addView(Button(this).apply {
            text = "Enable accessibility service"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        layout.addView(Button(this).apply {
            text = "Start refresh session"
            setOnClickListener {
                if (!saveSettings()) return@setOnClickListener
                if (!isAccessibilityEnabled()) {
                    Toast.makeText(this@MainActivity, "Enable TAPSTORM in Accessibility settings first", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    return@setOnClickListener
                }
                prefs.edit().putBoolean("automation_enabled", true).apply()
                status.text = "Armed. Open the selected app. Session stops on app/activity change, after 30 minutes, or 100 clicks."
                Toast.makeText(this@MainActivity, "Session armed. Now open the selected app.", Toast.LENGTH_LONG).show()
            }
        })
        layout.addView(Button(this).apply {
            text = "STOP NOW"
            setOnClickListener {
                prefs.edit().putBoolean("automation_enabled", false).apply()
                TapstormAccessibilityService.requestStop()
                status.text = "Stopped"
            }
        })
        layout.addView(TextView(this).apply {
            text = "\nSafety limits:\n• Only the exact labels Refresh / Reload\n• Only the package name you enter\n• Visible foreground app only\n• Stops on app/activity change\n• Maximum 30 minutes or 100 clicks\n• Never clicks payment, transfer, approve, OTP, PIN, or confirmation controls"
            textSize = 13f
        })
        setContentView(layout)
    }

    private fun saveSettings(): Boolean {
        val pkg = packageInput.text.toString().trim()
        val interval = intervalInput.text.toString().toLongOrNull()
        if (pkg.isBlank() || !pkg.contains(".")) {
            Toast.makeText(this, "Enter the target app package name", Toast.LENGTH_LONG).show()
            return false
        }
        if (interval == null || interval !in 10L..300L) {
            Toast.makeText(this, "Interval must be 10–300 seconds", Toast.LENGTH_LONG).show()
            return false
        }
        prefs.edit()
            .putString("target_package", pkg)
            .putLong("interval_seconds", interval)
            .apply()
        return true
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, TapstormAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}