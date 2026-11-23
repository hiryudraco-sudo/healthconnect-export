package com.healthconnect.export

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup

/**
 * Privacy rationale entry point required by the Health Connect APK on Android 13.
 * No health data is accessed from this activity.
 */
class HealthPermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        layout.addView(TextView(this).apply {
            text = "Health Connect data access"
            textSize = 22f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(TextView(this).apply {
            text = "Health Connect Export reads only the health data types you select, such as steps, to generate exports on your device. Data is shared with external services only when you explicitly configure an export destination. You can revoke access in Health Connect settings at any time."
            textSize = 16f
            setPadding(0, padding, 0, 0)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(layout)
    }
}
