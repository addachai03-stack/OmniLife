package com.tapstorm.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("tapstorm_timer", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var intervalInput: EditText
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private var running = false
    private var intervalSeconds = 30L
    private var remainingSeconds = 0L

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            remainingSeconds--
            if (remainingSeconds <= 0L) {
                status.text = "Interval complete. Start the next interval when ready."
                running = false
                startButton.isEnabled = true
                Toast.makeText(this@MainActivity, "TAPSTORM timer interval complete", Toast.LENGTH_SHORT).show()
                return
            }
            status.text = "Timer running — $remainingSeconds seconds remaining"
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 40)
        }
        layout.addView(TextView(this).apply {
            text = "TAPSTORM"
            textSize = 30f
        })
        layout.addView(TextView(this).apply {
            text = "Simple interval timer"
            textSize = 17f
            setPadding(0, 8, 0, 24)
        })
        intervalInput = EditText(this).apply {
            hint = "Interval in seconds (10–3600)"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getLong("interval_seconds", 30L).toString())
        }
        layout.addView(intervalInput)
        status = TextView(this).apply {
            text = "Set an interval and press Start."
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }
        layout.addView(status)
        startButton = Button(this).apply {
            text = "START TIMER"
            setOnClickListener { startTimer() }
        }
        layout.addView(startButton)
        layout.addView(Button(this).apply {
            text = "STOP TIMER"
            setOnClickListener { stopTimer("Timer stopped.") }
        })
        layout.addView(TextView(this).apply {
            text = "\nSafety: TAPSTORM only runs its own visible timer. It does not access, click, read, or control other apps. No Accessibility permission, payment access, or background automation is used."
            textSize = 14f
            setPadding(0, 24, 0, 0)
        })
        setContentView(layout)
    }

    private fun startTimer() {
        val value = intervalInput.text.toString().toLongOrNull()
        if (value == null || value !in 10L..3600L) {
            Toast.makeText(this, "Enter an interval from 10 to 3600 seconds", Toast.LENGTH_LONG).show()
            return
        }
        intervalSeconds = value
        prefs.edit().putLong("interval_seconds", value).apply()
        remainingSeconds = intervalSeconds
        running = true
        startButton.isEnabled = false
        status.text = "Timer running — $remainingSeconds seconds remaining"
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 1000L)
    }

    private fun stopTimer(message: String) {
        running = false
        handler.removeCallbacks(ticker)
        startButton.isEnabled = true
        status.text = message
    }

    override fun onDestroy() {
        stopTimer("Timer stopped.")
        super.onDestroy()
    }
}