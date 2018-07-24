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

package io.github.twoloops.weardocuments.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import io.github.twoloops.core.File
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.adapters.FileBrowserListAdapter
import io.github.twoloops.weardocuments.helpers.Utils
import io.github.twoloops.weardocuments.tasks.FileLoaderTask
import java.lang.ref.WeakReference

class FileBrowserDialog(context: Context, var selectedFiles: ArrayList<File>? = null) : Dialog(context) {

    private val filesList by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<RecyclerView>(R.id.file_browser_dialog_list)
    }
    private val selectButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.file_browser_dialog_select_button)
    }
    private val cancelButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.file_browser_dialog_cancel_button)
    }
    private val currentPathTextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.file_browser_dialog_path)
    }
    private val adapter: FileBrowserListAdapter by lazy(LazyThreadSafetyMode.NONE) {
        FileBrowserListAdapter()
    }
    private val progressBar by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ProgressBar>(R.id.file_browser_dialog_list_progress)
    }
    private var rootDirectory = Environment.getExternalStorageDirectory()
    private var currentPath = rootDirectory

    var listener: ((item: ArrayList<File>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.file_browser_dialog)
        initializeFilesList()
        initializeCancelButton()
        initializeSelectButton()
        setOnShowListener {
            currentPath = rootDirectory
            currentPathTextView.text = "${java.io.File.separator}${rootDirectory.toURI().relativize(currentPath.toURI()).path.toString()}"
            filesList.visibility = View.INVISIBLE
            loadFiles()
        }
    }

    private fun loadFiles() {
        FileLoaderTask(WeakReference(progressBar), {
            adapter.selectedItems = Utils.createSparseArrayForFiles(selectedFiles ?: ArrayList())
            currentPathTextView.text = "${java.io.File.separator}${rootDirectory.toURI().relativize(currentPath.toURI()).path.toString()}"
            if (it != null) {
                with(adapter) {
                    isRoot = rootDirectory.compareTo(java.io.File(currentPath.absolutePath)) == 0
                    itemList = it
                    notifyDataSetChanged()
                }
            }
            filesList.visibility = View.VISIBLE
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentPath)
    }

    private fun initializeCancelButton() {
        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun initializeSelectButton() {
        selectButton.isEnabled = selectedFiles != null
        if (selectedFiles != null) {
            selectButton.text = "${context.resources.getString(R.string.file_browser_dialog_select)} (${selectedFiles!!.count()})"
        }
        selectButton.setOnClickListener {
            val items = ArrayList<File>()
            for (i in 0 until adapter.selectedItems.size()) {
                items.add(adapter.selectedItems.valueAt(i))
            }
            listener?.invoke(items)
            dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeFilesList() {
        filesList.layoutManager = LinearLayoutManager(context)
        adapter.listener = { item ->
            if (item == null) {
                currentPath = java.io.File(currentPath.absolutePath.substringBeforeLast(java.io.File.separator, ""))
                loadFiles()
            } else {
                if (item.type == File.FILE_TYPE_FOLDER) {
                    currentPath = java.io.File(item.path)
                    loadFiles()
                } else {
                    val selectedCount = adapter.selectedItems.size()
                    if (selectedCount > 0) {
                        selectButton.isEnabled = true
                        selectButton.text = "${context.resources.getString(R.string.file_browser_dialog_select)} ($selectedCount)"
                    } else {
                        selectButton.isEnabled = false
                        selectButton.text = context.resources.getString(R.string.file_browser_dialog_select)
                    }
                }
            }
        }
        filesList.adapter = adapter
    }
}