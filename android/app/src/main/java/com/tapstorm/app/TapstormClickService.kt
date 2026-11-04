package com.tapstorm.app

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

class TapstormClickService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val clickLoop = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("tapstorm_timer", MODE_PRIVATE)
            if (prefs.getBoolean("auto_click_enabled", false)) {
                val target = prefs.getString("selected_package", null)
                val root = rootInActiveWindow
                if (target != null && root?.packageName?.toString() == target) {
                    findSearchButton(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                val seconds = prefs.getLong("interval_seconds", 10L).coerceIn(5L, 3600L)
                handler.postDelayed(this, seconds * 1000L)
            } else {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.removeCallbacks(clickLoop)
        handler.post(clickLoop)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { handler.removeCallbacks(clickLoop) }

    private fun findSearchButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count < 500) {
            val node = queue.removeFirst()
            count++
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (text.equals("Search", true) || desc.equals("Search", true)) {
                if (node.isClickable) return node
                var parent = node.parent
                var depth = 0
                while (parent != null && depth < 3) {
                    if (parent.isClickable) return parent
                    parent = parent.parent
                    depth++
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }
}
