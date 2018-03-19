package io.github.twoloops.weardocuments.views

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import io.github.twoloops.weardocuments.R
import java.math.BigDecimal


class SettingsView : Activity() {

    private val sharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getSharedPreferences("$packageName.preferences", Context.MODE_PRIVATE)
    }
    private val zoomLevel by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.settings_view_zoom_level)
    }
    private val zoomLevelIncrease by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.settings_view_increase_zoom_level)
    }
    private val zoomLevelDecrease by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.settings_view_decrease_zoom_level)
    }
    private val zoomStrength by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.settings_view_zoom_strength)
    }
    private val zoomStrengthIncrease by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.settings_view_increase_zoom_strength)
    }
    private val zoomStrengthDecrease by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.settings_view_decrease_zoom_strength)
    }
    private val darkModeSwitch by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Switch>(R.id.settings_view_dark_mode)
    }
    private var currentZoomLevel = 2
    private var currentZoomStrength = 1f
    private var darkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_view)
        currentZoomLevel = sharedPreferences.getInt("ZoomLevel", 2)
        setZoomLevelText()
        zoomLevelIncrease.setOnClickListener({
            if (currentZoomLevel < 4) {
                currentZoomLevel++
                setZoomLevelText()
            }
        })
        zoomLevelDecrease.setOnClickListener({
            if (currentZoomLevel > 1) {
                currentZoomLevel--
                setZoomLevelText()
            }
        })
        currentZoomStrength = sharedPreferences.getFloat("ZoomStrength", 1f)
        setZoomStrengthText()
        zoomStrengthIncrease.setOnClickListener({
            if (currentZoomStrength < 2f) {
                currentZoomStrength += 0.1f
                setZoomStrengthText()
            }
        })
        zoomStrengthDecrease.setOnClickListener({
            if (currentZoomStrength > 0.5f) {
                currentZoomStrength -= 0.1f
                setZoomStrengthText()
            }
        })
        darkMode = sharedPreferences.getBoolean("DarkMode", false)
        setDarkModeSwitch()
        darkModeSwitch.setOnClickListener {
            darkMode = darkModeSwitch.isChecked
        }
    }

    private fun setZoomStrengthText() {
        zoomStrength.text = "${BigDecimal(currentZoomStrength.toDouble()).setScale(2, BigDecimal.ROUND_HALF_UP).toFloat()}"
    }

    private fun setZoomLevelText() {
        zoomLevel.text = "$currentZoomLevel"
    }

    private fun setDarkModeSwitch() {
        darkModeSwitch.isChecked = darkMode
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit().putInt("ZoomLevel", currentZoomLevel).apply()
        sharedPreferences.edit().putFloat("ZoomStrength", currentZoomStrength).apply()
        sharedPreferences.edit().putBoolean("DarkMode", darkMode).apply()
        setResult(RESULT_OK)
    }
}