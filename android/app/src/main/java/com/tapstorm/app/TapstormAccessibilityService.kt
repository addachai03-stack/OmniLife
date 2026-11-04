package com.tapstorm.app

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Strictly limited foreground helper:
 * - exact package chosen by the user
 * - only visible controls named "Refresh" or "Reload"
 * - stops on activity/window-class changes, app switch, timeout, or click cap
 */
class TapstormAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var targetPackage: String? = null
    private var observedWindowClass: CharSequence? = null
    private var intervalMs = 30_000L
    private var startedAt = 0L
    private var clickCount = 0

    private val runner = object : Runnable {
        override fun run() {
            if (!active) return
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ENABLED, false)) { stopSession(); return }
            if (SystemClock.elapsedRealtime() - startedAt >= MAX_SESSION_MS || clickCount >= MAX_CLICKS) {
                stopSession(); return
            }
            val root = rootInActiveWindow
            if (root == null || root.packageName?.toString() != targetPackage) {
                stopSession(); return
            }
            val clicked = findAndClickAllowedControl(root)
            if (clicked) clickCount++
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        val configuredPackage = prefs.getString(KEY_PACKAGE, null)?.trim().orEmpty()
        if (configuredPackage.isBlank()) { stopSession(); return }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val eventPackage = event.packageName?.toString()
            if (eventPackage != configuredPackage) {
                if (active) stopSession()
                return
            }
            val newClass = event.className
            if (!active) {
                targetPackage = configuredPackage
                observedWindowClass = newClass
                intervalMs = prefs.getLong(KEY_INTERVAL, 30L).coerceIn(10L, 300L) * 1000L
                startedAt = SystemClock.elapsedRealtime()
                clickCount = 0
                active = true
                handler.postDelayed(runner, intervalMs)
            } else if (newClass != observedWindowClass) {
                stopSession()
            }
        }
    }

    private fun findAndClickAllowedControl(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited++ < 400) {
            val node = queue.removeFirst()
            val label = node.text?.toString()?.trim()
                ?: node.contentDescription?.toString()?.trim()
            if (node.isVisibleToUser && node.isEnabled &&
                (label.equals("Refresh", true) || label.equals("Reload", true))) {
                var candidate: AccessibilityNodeInfo? = node
                var depth = 0
                while (candidate != null && depth++ < 5) {
                    if (candidate.isClickable && candidate.isEnabled && candidate.isVisibleToUser) {
                        return candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    candidate = candidate.parent
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return false
    }

    private fun stopSession() {
        active = false
        handler.removeCallbacks(runner)
        targetPackage = null
        observedWindowClass = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, false).apply()
    }

    override fun onInterrupt() = stopSession()

    override fun onDestroy() {
        stopSession()
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val PREFS = "tapstorm"
        private const val KEY_ENABLED = "automation_enabled"
        private const val KEY_PACKAGE = "target_package"
        private const val KEY_INTERVAL = "interval_seconds"
        private const val MAX_SESSION_MS = 30 * 60 * 1000L
        private const val MAX_CLICKS = 100
        @Volatile private var instance: TapstormAccessibilityService? = null

        fun requestStop() { instance?.stopSession() }
    }
}