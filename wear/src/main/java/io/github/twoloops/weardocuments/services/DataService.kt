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

package io.github.twoloops.weardocuments.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.twoloops.core.Document
import io.github.twoloops.core.SerializableBitmap
import io.github.twoloops.core.SingletonHolder
import org.json.JSONArray
import java.io.*
import java.util.concurrent.Executors

class DataService(private val context: Context) {

    private val fileName = "data.json"
    private var currentDataJson: JSONArray? = null
    private val executor = Executors.newSingleThreadExecutor()
    var listener: ((json: JSONArray, index: Int) -> Unit)? = null

    private fun saveImages(document: Document) {
        "${context.filesDir.absolutePath}${File.separator}${document.id}${File.separator}"
        val folder = context.getDir(document.id, Context.MODE_PRIVATE)
        for (i in document.dataChunkStart until Math.min((document.dataChunkStart + document.data.count()), document.dataCount)) {
            val file = File(folder, "$i")
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            val out = FileOutputStream(file)
            document.data[i - document.dataChunkStart].currentImage!!.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
        }
    }

    fun deleteImages(document: Document) {
        "${context.filesDir.absolutePath}${File.separator}${document.id}${File.separator}"
        val folder = context.getDir(document.id, Context.MODE_PRIVATE)
        try {
            for (i in 0 until document.dataCount) {
                val file = File(folder, "$i")
                if (file.exists()) {
                    file.delete()
                }
            }
            folder.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadJson(): JSONArray {
        val fileName = "data.json"
        val file = java.io.File("${context.filesDir}${java.io.File.separator}$fileName")
        currentDataJson = JSONArray()
        try {
            val jsonString = String(file.readBytes())
            if (!jsonString.isBlank()) {
                currentDataJson = JSONArray(jsonString)
            }
        } catch (exception: FileNotFoundException) {
            file.createNewFile()
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(currentDataJson.toString().toByteArray())
                it.close()
            }
        }
        return currentDataJson!!
    }

    fun loadDocumentData(document: Document) {
        val folder = context.getDir(document.id, Context.MODE_PRIVATE)
        for (index in 0..(document.dataCount - 1)) {
            val file = File(folder, "$index")
            if (file.exists()) {
                val `in` = FileInputStream(file)
                document.data.add(SerializableBitmap(BitmapFactory.decodeStream(`in`)))
                `in`.close()
            }
        }
    }

    fun loadDocumentImages(document: Document) {
        val folder = context.getDir(document.id, Context.MODE_PRIVATE)
        for (index in 0..(document.dataCount - 1)) {
            val file = File(folder, "$index")
            if (file.exists()) {
                document.imageFiles.add(file)
            }
        }
    }

    fun addToJson(objectInputStream: ObjectInputStream) {
        executor.submit({
            addToJson(objectInputStream.readObject() as Document)
        })
    }

    fun addToJson(document: Document) {
        saveImages(document)
        if (document.dataChunkStart + document.dataChunkSize >= document.dataCount) {
            loadJson()
            if (currentDataJson == null) {
                currentDataJson = JSONArray()
            }
            var indexOfDocument = (0..(currentDataJson!!.length() - 1)).lastOrNull { currentDataJson!!.getJSONObject(it).getString("id") == document.id }
                    ?: -1
            if (indexOfDocument == -1) {
                currentDataJson!!.put(document.toJsonObject())
                indexOfDocument = currentDataJson!!.length() - 1
            } else {
                currentDataJson!!.put(indexOfDocument, document.toJsonObject())
            }
            with(context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                write(currentDataJson.toString().toByteArray())
                close()
            }
            context.mainLooper.run {
                listener?.invoke(currentDataJson!!, indexOfDocument)
            }
        }
    }

    fun removeFromJson(documentId: String): Boolean {
        try {
            loadJson()
            if (currentDataJson == null) {
                currentDataJson = JSONArray()
            }
            val indexOfDocument = (0..(currentDataJson!!.length() - 1)).lastOrNull { currentDataJson!!.getJSONObject(it).getString("id") == documentId }
                    ?: -1
            if (indexOfDocument != -1) {
                currentDataJson!!.remove(indexOfDocument)
            }
            with(context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                write(currentDataJson.toString().toByteArray())
                close()
                context.mainLooper.run {
                    listener?.invoke(currentDataJson!!, indexOfDocument)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    companion object : SingletonHolder<DataService, Context>(::DataService)
}