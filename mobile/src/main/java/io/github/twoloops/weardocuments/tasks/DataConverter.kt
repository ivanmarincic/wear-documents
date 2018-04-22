package io.github.twoloops.weardocuments.tasks

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.AsyncTask
import android.os.Build
import android.os.ParcelFileDescriptor
import io.github.twoloops.core.Document
import io.github.twoloops.core.File
import io.github.twoloops.core.SerializableBitmap
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.dialogs.AlertInfoDialog
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt
import android.R.attr.bitmap
import android.graphics.Canvas
import android.graphics.Color


class DataConverter(private val context: WeakReference<Context>, private var listener: ((ArrayList<SerializableBitmap>) -> Unit)? = null) : AsyncTask<Document, Boolean, ArrayList<SerializableBitmap>>() {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
                    if (bitmap != null) {
                        val scaleFactor = scaledWidth / bitmap.width
                        val newBitmap = SerializableBitmap(Bitmap.createScaledBitmap(bitmap, scaledWidth.roundToInt(), (bitmap.height * scaleFactor).roundToInt(), false))
                        result.add(newBitmap)
                    } else {
                        val conf = Bitmap.Config.ARGB_8888
                        val bmp = Bitmap.createBitmap(1, 1, conf)
                        val newBitmap = SerializableBitmap(bmp)
                        result.add(newBitmap)
                    }
                }
            }
            File.FILE_TYPE_PDF -> {
                try {
                    val pdfRenderer = PdfRenderer(ParcelFileDescriptor.open(java.io.File(document.files.first().path), ParcelFileDescriptor.MODE_READ_ONLY))
                    for (i in 0 until pdfRenderer.pageCount) {
                        val page = pdfRenderer.openPage(i)
                        val width = context.get()!!.resources.displayMetrics.densityDpi / 72 * page.width
                        val height = context.get()!!.resources.displayMetrics.densityDpi / 72 * page.height
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val scaleFactor = scaledWidth / width
                        val newBitmap = SerializableBitmap(Bitmap.createScaledBitmap(bitmap, scaledWidth.roundToInt(), (height * scaleFactor).roundToInt(), false))
                        result.add(newBitmap)
                        page.close()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_reading_file))
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_converting))
                }
            }
        }
        return result
    }

    override fun onPostExecute(result: ArrayList<SerializableBitmap>?) {
        listener?.invoke(result!!)
    }
}