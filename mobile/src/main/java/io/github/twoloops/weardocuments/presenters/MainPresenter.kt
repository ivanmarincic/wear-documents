package io.github.twoloops.weardocuments.presenters

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.contracts.MainContract
import io.github.twoloops.weardocuments.tasks.DocumentDeleteTask
import io.github.twoloops.weardocuments.tasks.DocumentUploadTask
import io.github.twoloops.weardocuments.views.MainView
import org.json.JSONArray
import java.io.FileNotFoundException
import java.lang.ref.WeakReference


class MainPresenter : MainContract.Presenter {

    override fun getFile() {
    }

    private lateinit var view: MainView

    override fun start(view: MainContract.View) {
        this.view = view as MainView
    }

    override fun initializeAddButton() {
        view.addButton.isEnabled = hasPermission()
        view.addButton.setOnClickListener {
            it.isEnabled = false
            view.getFileBrowserDialog().show()
        }
    }

    override fun initializeList() {
        view.adapter.itemList = view.items!!
        view.adapter.listener = {
            view.getDocumentPreviewDialog(it).show()
        }
        with(view.fileList) {
            layoutManager = LinearLayoutManager(view)
            itemAnimator = DefaultItemAnimator()
            adapter = view.adapter
        }
    }

    override fun hasPermission(): Boolean {
        val permissionStatus = ActivityCompat.checkSelfPermission(view,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        if (!permissionStatus) {
            ActivityCompat.requestPermissions(view,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    view.READ_STORAGE_REQUEST_CODE)
        }
        return permissionStatus
    }

    override fun sendDocument(document: Document) {
        DocumentUploadTask(WeakReference(view)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, document)
    }

    override fun deleteDocument(document: Document, listener: ((isSuccessful: Boolean) -> Unit)) {
        DocumentDeleteTask(WeakReference(view), listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, document)
    }

    override fun loadData() {
        val fileName = "data.json"
        val file = java.io.File("${view.filesDir}${java.io.File.separator}$fileName")
        try {
            val jsonString = String(file.readBytes())
            if (jsonString.isBlank()) {
                view.items = ArrayList()
            } else {
                val arrayList = ArrayList<Document>()
                val jsonArray = JSONArray(jsonString)
                for (i in 0..(jsonArray.length() - 1)) {
                    val document = Document()
                    document.fromJSONObject(jsonArray.getJSONObject(i))
                    arrayList.add(document)
                }
                view.items = arrayList
            }
        } catch (exception: FileNotFoundException) {
            view.items = ArrayList()
            file.createNewFile()
        }
        view.checkDocumentCount()
    }

    override fun saveData() {
        val filename = "data.json"
        if (view.items != null) {
            view.openFileOutput(filename, Context.MODE_PRIVATE).use {
                val jsonArray = JSONArray()
                for (document in view.items!!) {
                    jsonArray.put(document.toJsonObject())
                }
                it.write(jsonArray.toString().toByteArray())
                it.close()
            }
        }
    }

    override fun initializeToolbar() {
    }
}