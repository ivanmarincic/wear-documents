package io.github.twoloops.weardocuments.tasks

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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


class DataConverter(private val context: WeakReference<Context>, private var listener: ((ArrayList<SerializableBitmap>?) -> Unit)? = null) : AsyncTask<Document, Boolean, ArrayList<SerializableBitmap>>() {

    private val sharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.get()!!.getSharedPreferences("${context.get()!!.packageName}.preferences", Context.MODE_PRIVATE)
    }

    private val chunkSize = 10

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun doInBackground(vararg params: Document?): ArrayList<SerializableBitmap>? {
        var isSuccessful = true
        val document = params[0]!!
        val result: ArrayList<SerializableBitmap> = ArrayList()
        var scaleFactor = (sharedPreferences.getInt("ImageQuality", 0) + 50) / 100f
        try {
            when (document.type) {
                File.FILE_TYPE_IMAGE -> {
                    val count = document.files.count()
                    for (i in 0 until Math.ceil((count / chunkSize).toDouble()).toInt()) {
                        val options = BitmapFactory.Options()
                        options.inPreferredConfig = Bitmap.Config.RGB_565
                        val bitmap = BitmapFactory.decodeFile(document.files[i].path, options)
                        if (bitmap != null) {
                            val newBitmap = SerializableBitmap(Bitmap.createScaledBitmap(bitmap, (bitmap.width * scaleFactor).roundToInt(), (bitmap.height * scaleFactor).roundToInt(), false))
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
                    val pdfRenderer = PdfRenderer(ParcelFileDescriptor.open(java.io.File(document.files.first().path), ParcelFileDescriptor.MODE_READ_ONLY))
                    for (i in 0 until Math.min(chunkSize, pdfRenderer.pageCount)) {
                        val page = pdfRenderer.openPage(i)
                        var width = context.get()!!.resources.displayMetrics.densityDpi / 72 * page.width
                        if (width > 800) {
                            scaleFactor = 800f / width
                        }
                        var height = context.get()!!.resources.displayMetrics.densityDpi / 72 * page.height
                        val bitmap = Bitmap.createBitmap((width * scaleFactor).roundToInt(), (height * scaleFactor).roundToInt(), Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        result.add(SerializableBitmap(bitmap))
                        page.close()
                    }
                }
            }
        } catch (e: IOException) {
            isSuccessful = false
            e.printStackTrace()
            AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_reading_file))
        } catch (e: RuntimeException) {
            isSuccessful = false
            e.printStackTrace()
            AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_converting))
        } catch (e: OutOfMemoryError) {
            isSuccessful = false
            e.printStackTrace()
            AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_oom))
        }
        return if (isSuccessful) {
            result
        } else {
            null
        }
    }

    override fun onPostExecute(result: ArrayList<SerializableBitmap>?) {
        listener?.invoke(result)
    }
}