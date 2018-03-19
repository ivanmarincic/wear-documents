package io.github.twoloops.weardocuments.tasks

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import io.github.twoloops.core.File
import java.lang.ref.WeakReference


class FileLoaderTask(private var progressBar: WeakReference<ProgressBar>, private var listener: ((ArrayList<File>?) -> Unit)? = null) : AsyncTask<java.io.File, Boolean, Unit>() {

    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }

    override fun onPreExecute() {
        handler.post({
            progressBar.get()!!.visibility = View.INVISIBLE
        })
    }

    override fun onPostExecute(result: Unit?) {
        handler.post({
            progressBar.get()!!.visibility = View.INVISIBLE
        })
    }

    override fun doInBackground(vararg params: java.io.File?) {
        val folder = params[0]!!
        if (folder.isDirectory) {
            val filesInFolder = folder.listFiles().filter {
                it.extension == "png"
                        || it.extension == "jpg"
                        || it.extension == "pdf"
                        || it.extension == "docx"
                        || it.extension == "png"
                        || it.extension == ""
            }
            val filesList = ArrayList<File>()
            for (fileEntry in filesInFolder) {
                val file = File()
                file.path = fileEntry.absolutePath
                if (fileEntry.isDirectory) {
                    file.type = File.FILE_TYPE_FOLDER
                } else {
                    file.type = when (fileEntry.extension) {
                        "png", "jpg" -> File.FILE_TYPE_IMAGE
                        "pdf" -> File.FILE_TYPE_PDF
                        "docx" -> File.FILE_TYPE_DOCUMENT
                        else -> 0
                    }
                }
                filesList.add(file)
            }
            handler.post({
                listener?.invoke(filesList)
            })
        } else {
            handler.post({
                listener?.invoke(null)
            })
        }
    }
}