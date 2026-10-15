package com.example.rapido

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("rapido_auto_prefs", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(40, 60, 40, 40)
        }

        // Title
        val title = TextView(this).apply {
            text = "⚡ Rapido Fast Auto-Clicker"
            textSize = 22f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "வீடியோ பார்த்துக்கொண்டிருக்கும் போதும் ஆர்டர் வந்த உடனே தானாக கிளிக் செய்யும் (100% Free Open Source)"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 10, 0, 40)
        }
        root.addView(subtitle)

        // Master Auto-Accept Switch Card
        var isAutoOn = prefs.getBoolean("is_auto_accept_on", true)
        val masterSwitch = Switch(this).apply {
            text = "ஆட்டோ-அக்செப்ட் ஸ்விட்ச் (Auto-Accept)"
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = isAutoOn
            setOnCheckedChangeWidgetListener { _, checked ->
                isAutoOn = checked
                prefs.edit().putBoolean("is_auto_accept_on", checked).apply()
            }
            setPadding(20, 30, 20, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }
        root.addView(masterSwitch)

        // Accessibility Permission Button
        val accessibilityBtn = Button(this).apply {
            text = "1. Accessibility அனுமதி ஆன் செய்க (Turn ON Accessibility)"
            setBackgroundColor(Color.parseColor("#10B981"))
            setTextColor(Color.BLACK)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 40, 0, 15) }
        }
        root.addView(accessibilityBtn)

        // Floating Widget Overlay Button
        val overlayBtn = Button(this).apply {
            text = "2. மிதக்கும் ஸ்விட்ச் ஆன் செய்க (Floating Switch over Video)"
            setBackgroundColor(Color.parseColor("#3B82F6"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    startService(Intent(this@MainActivity, FloatingSwitchService::class.java))
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 30) }
        }
        root.addView(overlayBtn)

        // Optional Min Fare Filter
        val fareLabel = TextView(this).apply {
            text = "குறைந்தபட்ச கட்டணம் (Min Fare ₹) - 0 எனில் அனைத்து ரைடுகளையும் ஏற்கும்:"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
        }
        root.addView(fareLabel)

        val fareInput = EditText(this).apply {
            hint = "0"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setText(prefs.getInt("min_fare", 0).toString())
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(30, 20, 30, 20)
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val fare = text.toString().toIntOrNull() ?: 0
                    prefs.edit().putInt("min_fare", fare).apply()
                }
            }
        }
        root.addView(fareInput)

        setContentView(root)
    }
}
