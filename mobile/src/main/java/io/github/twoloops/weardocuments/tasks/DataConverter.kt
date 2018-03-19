package io.github.twoloops.weardocuments.tasks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import io.github.twoloops.core.Document
import io.github.twoloops.core.File
import io.github.twoloops.core.SerializableBitmap
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.dialogs.AlertInfoDialog
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt


class DataConverter(private val context: WeakReference<Context>, private var listener: ((ArrayList<SerializableBitmap>) -> Unit)? = null) : AsyncTask<Document, Boolean, ArrayList<SerializableBitmap>>() {

    override fun doInBackground(vararg params: Document?): ArrayList<SerializableBitmap> {
        val document = params[0]!!
        val result: ArrayList<SerializableBitmap> = ArrayList()
        val scaledWidth = 500f
        when (document.type) {
            File.FILE_TYPE_IMAGE -> {
                for (i in 0..(document.files.count() - 1)) {
                    val options = BitmapFactory.Options()
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    val bitmap = BitmapFactory.decodeFile(document.files[i].path, options)
                    val scaleFactor = scaledWidth / bitmap.width
                    val newBitmap = SerializableBitmap(Bitmap.createScaledBitmap(bitmap, scaledWidth.roundToInt(), (bitmap.height * scaleFactor).roundToInt(), false))
                    result.add(newBitmap)
                }
            }
            File.FILE_TYPE_PDF -> {
                PDFBoxResourceLoader.init(context.get()!!)
                try {
                    val pdDoc = PDDocument.load(java.io.File(document.files.first().path).inputStream())
                    val renderer = PDFRenderer(pdDoc)
                    for (i in 0..(pdDoc.numberOfPages - 1)) {
                        val bitmap = renderer.renderImage(i, 1f, Bitmap.Config.RGB_565)
                        val scaleFactor = scaledWidth / bitmap.width
                        val newBitmap = SerializableBitmap(Bitmap.createScaledBitmap(bitmap, scaledWidth.roundToInt(), (bitmap.height * scaleFactor).roundToInt(), false))
                        result.add(newBitmap)
                    }
                    pdDoc.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_reading_file))
                }
            }
        }
        return result
    }

    override fun onPostExecute(result: ArrayList<SerializableBitmap>?) {
        listener?.invoke(result!!)
    }
}