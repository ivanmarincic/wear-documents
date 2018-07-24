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

package io.github.twoloops.weardocuments.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.SparseArray
import io.github.twoloops.core.File
import io.github.twoloops.weardocuments.R
import java.util.concurrent.atomic.AtomicInteger


class Utils {
    companion object {
        private var atomicInteger = AtomicInteger(18273)
        var pageWidth: Int = 595
        var pageHeight: Int = 842

        fun getIconForType(type: Int): Int {
            return when (type) {
                File.FILE_TYPE_IMAGE -> R.drawable.ic_image
                File.FILE_TYPE_PDF -> R.drawable.ic_pdf
                File.FILE_TYPE_DOCUMENT -> R.drawable.ic_document
                File.FILE_TYPE_FOLDER -> R.drawable.ic_folder
                else -> 0
            }
        }

        fun getNotificationId(): Int {
            return atomicInteger.incrementAndGet()
        }

        fun createHashMapForFiles(files: ArrayList<File>): HashMap<Int, File> {
            val hashMap = HashMap<Int, File>()
            for (file: File in files) {
                hashMap[file.hashCode()] = file
            }
            return hashMap
        }

        fun createSparseArrayForFiles(files: ArrayList<File>): SparseArray<File> {
            val sparseArray = SparseArray<File>()
            for (file: File in files) {
                sparseArray.put(file.hashCode(), file);
            }
            return sparseArray
        }

        fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
            var drawable = ContextCompat.getDrawable(context, drawableId)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = DrawableCompat.wrap(drawable!!).mutate()
            }

            val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth,
                    drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
            drawable.draw(canvas)

            return bitmap
        }

        fun calculateInSampleSize(
                options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {

                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        fun decodeBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeFile(file.path, options)
        }
    }
}