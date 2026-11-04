package com.tapstorm.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Deliberately inert starter. Does not inspect app content or click controls.
 * Any future implementation requires a narrowly scoped, non-financial use case.
 */
class TapstormAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally no-op.
    }

    override fun onInterrupt() {
        // Intentionally no-op.
    }
}