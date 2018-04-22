package io.github.twoloops.weardocuments.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.DefaultItemAnimator
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

class FileBrowserDialog(context: Context, private var selectedFiles: ArrayList<File>? = null) : Dialog(context) {

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
            items.addAll(adapter.selectedItems.values)
            listener?.invoke(items)
            dismiss()
        }
    }

    private fun setItemsForFilesList() {
        filesList.visibility = View.INVISIBLE
        FileLoaderTask(WeakReference(progressBar), {
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

    @SuppressLint("SetTextI18n")
    private fun initializeFilesList() {
        FileLoaderTask(WeakReference(progressBar), {
            val listAdapter = adapter
            listAdapter.itemList = it
            listAdapter.listener = { item ->
                if (item == null) {
                    currentPath = java.io.File(currentPath.absolutePath.substringBeforeLast(java.io.File.separator, ""))
                    setItemsForFilesList()
                } else {
                    if (item.type == File.FILE_TYPE_FOLDER) {
                        currentPath = java.io.File(item.path)
                        setItemsForFilesList()
                    } else {
                        val selectedCount = adapter.selectedItems.count()
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
            if (selectedFiles != null) {
                listAdapter.selectedItems = Utils.createHashMapForFiles(selectedFiles!!)
            }
            with(filesList) {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = listAdapter
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentPath)
    }
}