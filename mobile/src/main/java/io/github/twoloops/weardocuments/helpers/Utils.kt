package io.github.twoloops.weardocuments.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import io.github.twoloops.core.File
import io.github.twoloops.weardocuments.R
import java.util.concurrent.atomic.AtomicInteger
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.os.Build
import android.support.v4.graphics.drawable.DrawableCompat
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION.SDK_INT
import android.support.v4.content.ContextCompat


class Utils {
    companion object {
        private var atomicInteger = AtomicInteger(18273)
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
    }
}