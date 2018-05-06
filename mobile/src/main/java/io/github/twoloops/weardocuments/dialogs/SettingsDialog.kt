package io.github.twoloops.weardocuments.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import io.github.twoloops.weardocuments.R

class SettingsDialog(context: Context) : Dialog(context) {

    private val qualitySettingsSlider by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<SeekBar>(R.id.settings_dialog_quality_slider)
    }

    private val qualitySettingsValue by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.settings_dialog_quality_value)
    }

    private val sharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.getSharedPreferences("${context.packageName}.preferences", Context.MODE_PRIVATE)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_dialog)
        qualitySettingsSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                qualitySettingsValue.text = "${progress + 50}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sharedPreferences.edit().putInt("ImageQuality", seekBar!!.progress).apply()
            }

        })
        qualitySettingsSlider.progress = sharedPreferences.getInt("ImageQuality", 0)
        qualitySettingsValue.text = "${qualitySettingsSlider.progress + 50}%"

    }
}