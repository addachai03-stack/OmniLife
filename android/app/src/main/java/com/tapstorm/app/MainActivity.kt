package com.tapstorm.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 40)
        }
        layout.addView(TextView(this).apply {
            text = "TAPSTORM\n\nAndroid companion scaffold\n\nTimer and safety controls are under development."
            textSize = 20f
        })
        layout.addView(Button(this).apply {
            text = "Open accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        layout.addView(TextView(this).apply {
            text = "\nNo click automation is enabled in this build."
            textSize = 14f
        })
        setContentView(layout)
    }
}