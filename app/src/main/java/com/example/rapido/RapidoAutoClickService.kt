package com.example.rapido

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RapidoAutoClickService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private var lastClickTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("rapido_auto_prefs", Context.MODE_PRIVATE)
        isServiceRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Check if master switch is ON
        val isEnabled = prefs.getBoolean("is_auto_accept_on", true)
        if (!isEnabled) return

        val rootNode = rootInActiveWindow ?: return

        // Throttle clicks to avoid multi-clicking the same order
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 2500) return

        // Look for Rapido order indicators: Rupee symbol '₹', 'Rs', 'Accept', 'ஏற்றுக்கொள்'
        val foundRupee = containsRupeeOrFare(rootNode)
        val acceptNode = findAcceptButton(rootNode)

        if (acceptNode != null) {
            // Check minimum fare filter if configured
            val minFare = prefs.getInt("min_fare", 0)
            if (minFare > 0 && foundRupee) {
                val extractedFare = extractFareAmount(rootNode)
                if (extractedFare in 1 until minFare) {
                    // Fare is below driver's threshold, skip click
                    return
                }
            }

            // Click accept button immediately
            val clicked = clickNode(acceptNode)
            if (clicked) {
                lastClickTime = now
                vibratePhone()
            }
        }
    }

    private fun findAcceptButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val searchTerms = arrayOf(
            "Accept", "ACCEPT", "ஏற்றுக்கொள்", "स्वीकार करें", "ಸ್ವೀಕರಿಸಿ", "అంగీకరించు"
        )

        for (term in searchTerms) {
            val nodes = root.findAccessibilityNodeInfosByText(term)
            for (node in nodes) {
                if (node.isVisibleToUser) {
                    // Climb up to find clickable parent if this node isn't directly clickable
                    var target: AccessibilityNodeInfo? = node
                    while (target != null && !target.isClickable) {
                        target = target.parent
                    }
                    if (target != null && target.isClickable) {
                        return target
                    }
                    return node
                }
            }
        }

        // Search by view IDs common in captain apps
        val idTerms = arrayOf("accept", "btn_accept", "acceptOffer", "accept_order")
        for (id in idTerms) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (node.isVisibleToUser) {
                    return node
                }
            }
        }

        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        // Try native action click
        var clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!clicked && node.parent != null) {
            clicked = node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return clicked
    }

    private fun containsRupeeOrFare(root: AccessibilityNodeInfo): Boolean {
        val rupeeNodes = root.findAccessibilityNodeInfosByText("₹")
        if (rupeeNodes.isNotEmpty()) return true
        val rsNodes = root.findAccessibilityNodeInfosByText("Rs")
        return rsNodes.isNotEmpty()
    }

    private fun extractFareAmount(root: AccessibilityNodeInfo): Int {
        val rupeeNodes = root.findAccessibilityNodeInfosByText("₹")
        for (node in rupeeNodes) {
            val text = node.text?.toString() ?: ""
            val digits = text.replace(Regex("[^0-9]"), "")
            if (digits.isNotEmpty()) {
                val amount = digits.toIntOrNull()
                if (amount != null && amount in 15..2000) {
                    return amount
                }
            }
        }
        return 0
    }

    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {}

    companion object {
        var isServiceRunning = false
    }
}
