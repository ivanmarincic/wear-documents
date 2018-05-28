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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import io.github.twoloops.core.Document
import io.github.twoloops.core.File
import io.github.twoloops.core.SerializableBitmap
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.dialogs.AlertInfoDialog
import io.github.twoloops.weardocuments.helpers.Utils.Companion.decodeBitmapFromFile
import io.github.twoloops.weardocuments.helpers.Utils.Companion.pageHeight
import io.github.twoloops.weardocuments.helpers.Utils.Companion.pageWidth
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


class DocumentConverter(private val document: Document, private val context: WeakReference<Context>) {

    var listener: ((ArrayList<SerializableBitmap>) -> Unit)? = null
    private var loopStart = 0
    private var loopEnd = 0

    init {
        loopStart = document.dataChunkStart
        loopEnd = document.dataChunkSize
    }

    fun nextChunk(): Boolean {
        return if (loopStart < document.dataCount - document.dataChunkSize) {
            loopStart += document.dataChunkSize
            loopEnd = Math.min(loopEnd + document.dataChunkSize, document.dataCount)
            document.dataChunkStart = loopStart
            processChunks()
            true
        } else {
            false
        }
    }

    fun previousChunk(): Boolean {
        return if (loopEnd > document.dataChunkSize) {
            loopStart = Math.max(0, loopStart - document.dataChunkSize)
            loopEnd -= document.dataChunkSize
            document.dataChunkStart = loopStart
            processChunks()
            true
        } else {
            false
        }
    }

    fun start() {
        processChunks()
    }

    private fun processChunks() {
        Thread {
            try {
                when (document.type) {
                    File.FILE_TYPE_PDF -> {
                        processPDF()
                    }
                    File.FILE_TYPE_IMAGE -> {
                        processImages()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_reading_file))
            } catch (e: RuntimeException) {
                e.printStackTrace()
                AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_converting))
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_oom))
            }
        }.start()
    }

    private fun processPDF() {
        val pdfRenderer = PdfRenderer(ParcelFileDescriptor.open(java.io.File(document.files.first().path), ParcelFileDescriptor.MODE_READ_ONLY))
        var scaleFactor = 1f
        document.dataCount = pdfRenderer.pageCount
        val result = ArrayList<SerializableBitmap>(document.dataCount)
        loopEnd = Math.min(document.dataCount, loopEnd)
        for (i in loopStart until loopEnd) {
            val page = pdfRenderer.openPage(i)
            val width = context.get()!!.resources.displayMetrics.densityDpi / 72 * page.width
            if (width > pageWidth) {
                scaleFactor = pageWidth.toFloat() / width
            }
            val height = context.get()!!.resources.displayMetrics.densityDpi / 72 * page.height
            val bitmap = Bitmap.createBitmap((width * scaleFactor).roundToInt(), (height * scaleFactor).roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            result.add(SerializableBitmap(bitmap))
            page.close()
        }
        pdfRenderer.close()
        listener?.invoke(result)
    }

    private fun processImages() {
        document.dataCount = document.files.size
        val result = ArrayList<SerializableBitmap>(document.dataCount)
        loopEnd = Math.min(document.dataCount, loopEnd)
        for (i in loopStart until loopEnd) {
            val bitmap = decodeBitmapFromFile(document.files[i], pageWidth, pageHeight)
            result.add(SerializableBitmap(bitmap))
        }
        listener?.invoke(result)
    }

    private fun decodeBitmapFromPDF() {

    }

}