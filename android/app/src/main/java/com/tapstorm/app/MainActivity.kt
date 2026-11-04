package com.tapstorm.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("tapstorm_timer", MODE_PRIVATE) }
    private lateinit var intervalInput: EditText
    private lateinit var status: TextView
    private lateinit var countdown: TextView
    private lateinit var selectedAppLabel: TextView
    private lateinit var startButton: Button
    private var running = false
    private var intervalSeconds = 10L
    private var remainingSeconds = 10L
    private var selectedPackage: String? = null

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            remainingSeconds--
            if (remainingSeconds <= 0L) {
                status.text = "⚡ Auto-click active • Selected app foreground mein hona chahiye"
                remainingSeconds = intervalSeconds
            }
            countdown.text = formatTime(remainingSeconds)
            if (running) handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intervalSeconds = prefs.getLong("interval_seconds", 10L).coerceIn(5L, 3600L)
        remainingSeconds = intervalSeconds
        selectedPackage = prefs.getString("selected_package", null)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
            background = gradient(intArrayOf(Color.rgb(5, 8, 20), Color.rgb(17, 12, 38)))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        content.addView(label("⚡ TAPSTORM", 30f, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(2)) })
        content.addView(label("APP CONNECT + REFRESH REMINDER", 13f, Color.rgb(255, 196, 45), true))
        content.addView(label("Choose an installed app to open when needed.", 15f, Color.LTGRAY, false).apply { setPadding(0, dp(5), 0, dp(18)) })

        val appPanel = panel()
        appPanel.addView(label("📱  CONNECT AN APP", 18f, Color.WHITE, true))
        selectedAppLabel = label("", 14f, Color.rgb(95, 230, 170), true).apply { setPadding(0, dp(10), 0, dp(8)) }
        appPanel.addView(selectedAppLabel)
        val appButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val addApp = Button(this).apply {
            text = "＋ ADD / CHANGE APP"
            setTextColor(Color.WHITE); background = rounded(Color.rgb(105, 45, 180), 18)
            setOnClickListener { showAppPicker() }
        }
        val openApp = Button(this).apply {
            text = "OPEN APP"
            setTextColor(Color.WHITE); background = rounded(Color.rgb(0, 145, 190), 18)
            setOnClickListener { openSelectedApp() }
        }
        appButtons.addView(addApp, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(5) })
        appButtons.addView(openApp, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(5) })
        appPanel.addView(appButtons)
        val remove = Button(this).apply {
            text = "REMOVE SELECTED APP"; setTextColor(Color.WHITE); background = rounded(Color.rgb(85, 45, 55), 16)
            setOnClickListener {
                selectedPackage = null
                prefs.edit().remove("selected_package").remove("selected_app_name").apply()
                updateSelectedApp()
            }
        }
        appPanel.addView(remove, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(8) })
        val accessibilityButton = Button(this).apply {
            text = "⚡ ENABLE AUTO-CLICK ACCESS"
            setTextColor(Color.WHITE); background = rounded(Color.rgb(205, 105, 0), 16)
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        appPanel.addView(accessibilityButton, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        content.addView(appPanel)
        updateSelectedApp()

        val settings = panel()
        settings.addView(label("⚙  Refresh interval", 20f, Color.WHITE, true))
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf(5, 10, 15, 30, 60).forEach { seconds ->
            val b = Button(this).apply {
                text = "${seconds}s"; textSize = 13f; setTextColor(Color.WHITE)
                background = rounded(if (intervalSeconds == seconds.toLong()) Color.rgb(226, 153, 0) else Color.rgb(42, 48, 70), 18)
                setOnClickListener { intervalInput.setText(seconds.toString()); chooseInterval(seconds.toLong()) }
            }
            presetRow.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), dp(12), dp(3), dp(12)) })
        }
        settings.addView(presetRow)
        intervalInput = EditText(this).apply {
            setText(intervalSeconds.toString()); hint = "Custom seconds (5–3600)"
            setHintTextColor(Color.LTGRAY); setTextColor(Color.WHITE); textSize = 17f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(14), 0, dp(14), 0); background = rounded(Color.rgb(28, 24, 48), 16)
        }
        settings.addView(intervalInput, LinearLayout.LayoutParams(-1, dp(52)))
        val apply = Button(this).apply {
            text = "APPLY INTERVAL"; setTextColor(Color.WHITE); background = rounded(Color.rgb(105, 45, 180), 18)
            setOnClickListener {
                val value = intervalInput.text.toString().toLongOrNull()
                if (value == null || value !in 5L..3600L) Toast.makeText(this@MainActivity, "5 se 3600 seconds ke beech set karo", Toast.LENGTH_LONG).show()
                else {
                    chooseInterval(value)
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(intervalInput.windowToken, 0)
                }
            }
        }
        settings.addView(apply, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })
        content.addView(settings)

        val live = panel()
        live.addView(label("LIVE COUNTDOWN", 15f, Color.rgb(65, 220, 255), true))
        countdown = label(formatTime(remainingSeconds), 48f, Color.rgb(75, 245, 130), true).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(8)) }
        live.addView(countdown)
        status = label("Ready — Start dabao.", 15f, Color.WHITE, false).apply { gravity = Gravity.CENTER }
        live.addView(status)
        content.addView(live)
        startButton = Button(this).apply {
            text = "▶  START AUTO-CLICK"; textSize = 18f; setTextColor(Color.WHITE); background = rounded(Color.rgb(0, 180, 75), 28)
            setOnClickListener { startReminder() }
        }
        content.addView(startButton, LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(8) })
        val stop = Button(this).apply {
            text = "■  STOP"; textSize = 17f; setTextColor(Color.WHITE); background = rounded(Color.rgb(210, 38, 48), 28)
            setOnClickListener { stopReminder() }
        }
        content.addView(stop, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(10) })
        val note = panel()
        note.addView(label("IMPORTANT", 15f, Color.rgb(255, 196, 45), true))
        note.addView(label("Auto-click sirf selected app foreground mein hone par exact Search button ko interval par click karega. Accessibility permission zaroori hai. App background/closed ho to click nahi hoga. STOP dabane par auto-click band ho jayega.", 14f, Color.LTGRAY, false))
        content.addView(note)
        content.addView(label("TAPSTORM  •  v0.5.0", 12f, Color.GRAY, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(18), 0, 0) })
        setContentView(scroll)
    }

    private fun showAppPicker() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if (apps.isEmpty()) {
            Toast.makeText(this, "Koi launchable app nahi mili", Toast.LENGTH_LONG).show()
            return
        }
        val labels = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select app to add")
            .setItems(labels) { _, which ->
                val info = apps[which]
                selectedPackage = info.activityInfo.packageName
                prefs.edit().putString("selected_package", selectedPackage)
                    .putString("selected_app_name", labels[which]).apply()
                updateSelectedApp()
                Toast.makeText(this, "${labels[which]} added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateSelectedApp() {
        val name = prefs.getString("selected_app_name", null)
        selectedAppLabel.text = if (selectedPackage == null) "No app selected — Add / Change App dabao" else "✓ Selected: ${name ?: selectedPackage}"
    }

    private fun openSelectedApp() {
        val pkg = selectedPackage
        if (pkg == null) {
            Toast.makeText(this, "Pehle Add / Change App se app select karo", Toast.LENGTH_LONG).show()
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch == null) {
            Toast.makeText(this, "Selected app abhi launch nahi ho pa raha", Toast.LENGTH_LONG).show()
            return
        }
        startActivity(launch)
    }

    private fun chooseInterval(value: Long) {
        intervalSeconds = value.coerceIn(5L, 3600L); remainingSeconds = intervalSeconds
        prefs.edit().putLong("interval_seconds", intervalSeconds).apply()
        countdown.text = formatTime(remainingSeconds); status.text = "Interval set: $intervalSeconds seconds"
        if (running) handler.removeCallbacks(ticker)
        running = false; startButton.isEnabled = true
    }

    private fun startReminder() {
        val value = intervalInput.text.toString().toLongOrNull()
        if (value == null || value !in 5L..3600L) {
            Toast.makeText(this, "5 se 3600 seconds set karo", Toast.LENGTH_LONG).show(); return
        }
        intervalSeconds = value; prefs.edit().putLong("interval_seconds", value).apply()
        remainingSeconds = intervalSeconds; running = true; startButton.isEnabled = false
        if (selectedPackage == null) {
            Toast.makeText(this, "Pehle Airtel Merchant select karo", Toast.LENGTH_LONG).show(); return
        }
        if (!isClickServiceEnabled()) {
            Toast.makeText(this, "Settings mein TAPSTORM Accessibility enable karo, phir Start dabao", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return
        }
        prefs.edit().putBoolean("auto_click_enabled", true).apply()
        status.text = "⚡ Auto-click ON • " + prefs.getString("selected_app_name", selectedPackage) + " foreground mein rakho"
        countdown.text = formatTime(remainingSeconds); handler.removeCallbacks(ticker); handler.postDelayed(ticker, 1000L)
    }

    private fun stopReminder() {
        running = false; handler.removeCallbacks(ticker); remainingSeconds = intervalSeconds
        prefs.edit().putBoolean("auto_click_enabled", false).apply()
        countdown.text = formatTime(remainingSeconds); status.text = "Auto-click stopped."; startButton.isEnabled = true
    }

    private fun isClickServiceEnabled(): Boolean {
        val expected = packageName + "/" + TapstormClickService::class.java.name
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return !TextUtils.isEmpty(enabled) && enabled!!.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) { }
    }
    private fun formatTime(seconds: Long): String = String.format("%02d:%02d", seconds / 60, seconds % 60)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun gradient(colors: IntArray) = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16))
        background = gradient(intArrayOf(Color.rgb(27, 32, 57), Color.rgb(15, 18, 35)))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(10), 0, dp(10)) }
    }
    private fun label(textValue: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = textValue; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    override fun onDestroy() { running = false; handler.removeCallbacks(ticker); super.onDestroy() }
}
