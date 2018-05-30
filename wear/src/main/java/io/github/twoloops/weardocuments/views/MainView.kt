/*
 * MIT License
 *
 * Copyright (c) 2018 Ivan Marinčić
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.twoloops.weardocuments.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wear.widget.drawer.WearableDrawerView
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.adapters.DocumentViewAdapter
import io.github.twoloops.weardocuments.contracts.DocumentAdapter
import io.github.twoloops.weardocuments.services.DataService
import org.json.JSONArray


class MainView : WearableActivity() {

    val SETTINGS_SAVED_REQUEST_CODE = 1
    val DOCUMENT_SELECTED_REQUEST_CODE = 2

    var currentDataJson: JSONArray = JSONArray()
    var currentItemIndex: Int = 0
    private val sharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getSharedPreferences("$packageName.preferences", Context.MODE_PRIVATE)
    }
    private val dataService = DataService.getInstance(this)
    private val contentView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<DocumentView>(R.id.main_layout_content)
    }
    private val drawer: WearableDrawerView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<WearableDrawerView>(R.id.main_layout_drawer)
    }
    private val settingsButton: ImageButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ImageButton>(R.id.main_layout_drawer_settings)
    }
    private val browseButton: ImageButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ImageButton>(R.id.main_layout_drawer_browse)
    }
    private val pageSlider: SeekBar by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<SeekBar>(R.id.main_layout_drawer_page_slider)
    }
    private val pageTexView: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.main_layout_drawer_page)
    }
    private val progressBar: ProgressBar by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ProgressBar>(R.id.main_layout_progress_bar)
    }
    private val emptyMessage: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.main_layout_empty)
    }
    private val pageIndicator: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.main_layout_page_indicator)
    }
    private val adapter: DocumentAdapter by lazy(LazyThreadSafetyMode.NONE) {
        DocumentViewAdapter(applicationContext)
    }
    private val handler: Handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }
    private var pageIndicatorRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_view)
        setAmbientEnabled()
        drawer.controller.peekDrawer()
        initializeContent()
        initializeSlider()
        settingsButton.setOnClickListener {
            startActivityForResult(Intent(this, SettingsView::class.java), SETTINGS_SAVED_REQUEST_CODE)
        }
        browseButton.setOnClickListener {
            startActivityForResult(Intent(this, DocumentBrowserView::class.java), DOCUMENT_SELECTED_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        dataService.listener = { dataJson, index ->
            currentDataJson = dataJson
            runOnUiThread {
                loadAtIndex(index)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dataService.listener = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SETTINGS_SAVED_REQUEST_CODE && (resultCode == RESULT_OK || resultCode == RESULT_CANCELED)) {
            loadDocumentLayoutSettings()
            adapter.refresh()
        }
        if (requestCode == DOCUMENT_SELECTED_REQUEST_CODE && resultCode == RESULT_OK) {
            loadAtIndex(data!!.getIntExtra("index", 0))
        }
        if (requestCode == DOCUMENT_SELECTED_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            currentDataJson = DataService.getInstance(this).loadJson()
            if (currentItemIndex >= currentDataJson.length()) {
                currentItemIndex = 0
            }
            if (currentDataJson.length() == 0) {
                currentItemIndex = -1
            }
            loadAtIndex(currentItemIndex)
        }
    }

    private fun loadDocumentLayoutSettings() {
        adapter.zoomLevels = sharedPreferences.getInt("ZoomLevel", 2)
        adapter.zoomStrength = sharedPreferences.getFloat("ZoomStrength", 1f)
        adapter.dark = sharedPreferences.getBoolean("DarkMode", false)
        adapter.setOnPageChangedListener {
            pageTexView.text = "${it + 1}"
            pageSlider.progress = it
            pageIndicator.text = "${it + 1}"
            pageIndicator.visibility = View.VISIBLE
            handler.removeCallbacks(pageIndicatorRunnable)
            pageIndicatorRunnable = Runnable {
                pageIndicator.visibility = View.INVISIBLE
            }
            handler.postDelayed(pageIndicatorRunnable, 3000)
        }
    }

    private fun loadAtIndex(index: Int) {
        progressBar.visibility = View.VISIBLE
        emptyMessage.visibility = View.INVISIBLE
        Thread(Runnable {
            try {
                if (currentDataJson.length() > 0 && currentItemIndex != -1) {
                    currentItemIndex = if (index < currentDataJson!!.length() && index >= 0) {
                        index
                    } else {
                        0
                    }
                    sharedPreferences.edit().putInt("ItemIndex", currentItemIndex).apply()
                    val newDocument = Document()
                    newDocument.fromJSONObject(currentDataJson!!.getJSONObject(currentItemIndex))
                    dataService.loadDocumentImages(newDocument)
                    runOnUiThread {
                        pageSlider.max = newDocument.dataCount - 1
                        pageSlider.progress = 0
                        adapter.document = newDocument
                    }
                } else {
                    runOnUiThread {
                        adapter.document = Document()
                        emptyMessage.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                runOnUiThread {
                    progressBar.visibility = View.INVISIBLE
                }
            }
        }).start()
    }

    private fun initializeSlider() {
        pageTexView.text = "1"
        pageSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                pageTexView.text = "${seekBar!!.progress + 1}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                adapter.currentPage = seekBar!!.progress
            }
        })
        pageSlider.setOnTouchListener { v, event ->
            currentDataJson.length() == 0
        }
    }

    private fun initializeContent() {
        currentDataJson = dataService.loadJson()
        loadDocumentLayoutSettings()
        contentView.adapter = adapter
        loadAtIndex(sharedPreferences.getInt("ItemIndex", 0))
    }
}