package io.github.twoloops.weardocuments.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.twoloops.core.Document
import io.github.twoloops.core.SerializableBitmap
import io.github.twoloops.core.SingletonHolder
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

class DataService(private val context: Context) {

    private val fileName = "data.json"
    private var currentDataJson: JSONArray? = null
    var listener: ((json: JSONArray, index: Int) -> Unit)? = null

    private fun saveImages(document: Document) {
        "${context.filesDir.absolutePath}${File.separator}${document.id}${File.separator}"
        val folder = context.getDir(document.id, Context.MODE_PRIVATE)
        for ((index, bitmap) in document.data.withIndex()) {
            val file = File(folder, "$index")
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            val out = FileOutputStream(file)
            bitmap.currentImage!!.compress(Bitmap.CompressFormat.JPEG, 50, out)
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

    fun addToJson(document: Document) {
        saveImages(document)
        loadJson()
        if (currentDataJson == null) {
            currentDataJson = JSONArray()
        }
        val indexOfDocument = (0..(currentDataJson!!.length() - 1)).lastOrNull { currentDataJson!!.getJSONObject(it).getString("id") == document.id }
                ?: -1
        if (indexOfDocument == -1) {
            currentDataJson!!.put(document.toJsonObject())
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
                var newIndex = indexOfDocument - 1
                if (newIndex < 0) {
                    newIndex = 0
                }
                context.mainLooper.run {
                    listener?.invoke(currentDataJson!!, newIndex)
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