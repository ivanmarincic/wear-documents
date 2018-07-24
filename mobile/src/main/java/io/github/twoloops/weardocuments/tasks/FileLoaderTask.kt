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

package io.github.twoloops.weardocuments.tasks

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import io.github.twoloops.core.File
import java.lang.ref.WeakReference


class FileLoaderTask(private var progressBar: WeakReference<ProgressBar>, private var listener: ((ArrayList<File>?) -> Unit)? = null) : AsyncTask<java.io.File, Boolean, ArrayList<File>?>() {

    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }

    override fun onPreExecute() {
        handler.post({
            progressBar.get()!!.visibility = View.VISIBLE
        })
    }

    override fun onPostExecute(result: ArrayList<File>?) {
        listener?.invoke(result)
        handler.post({
            progressBar.get()!!.visibility = View.INVISIBLE
        })
    }

    override fun doInBackground(vararg params: java.io.File?): ArrayList<File>? {
        val folder = params[0]!!
        if (folder.isDirectory) {
            val filesInFolder = folder.listFiles().filter {
                it.extension == "png"
                        || it.extension == "jpg"
                        || it.extension == "pdf"
                        || it.extension == "docx"
                        || it.extension == "png"
                        || it.isDirectory
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
            return filesList
        } else {
            return null
        }
    }
}