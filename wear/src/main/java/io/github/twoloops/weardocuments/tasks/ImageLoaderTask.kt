package io.github.twoloops.weardocuments.tasks

import android.graphics.*
import android.os.AsyncTask
import android.widget.ImageView
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference


class ImageLoaderTask(private val file: File, private val darkMode: Boolean) : AsyncTask<ImageView, Unit, Bitmap>() {
    private lateinit var view: WeakReference<ImageView?>

    override fun doInBackground(vararg params: ImageView?): Bitmap {
        view = WeakReference(params[0])
        val `in` = FileInputStream(file)
        val result = BitmapFactory.decodeStream(`in`)
        `in`.close()
        return if (darkMode) {
            applyFilters(result)
        } else {
            result
        }
    }

    override fun onPostExecute(result: Bitmap) {
        super.onPostExecute(result)
        view.get()?.setImageBitmap(result)
    }

    private fun applyFilters(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas()
        val paint = Paint()
        val invertedColorMatrix = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
        val sepiaColorFilter = ColorMatrixColorFilter(invertedColorMatrix)
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        canvas.setBitmap(result)
        paint.isFilterBitmap = false
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        paint.colorFilter = sepiaColorFilter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        bitmap.recycle()
        return result
    }
}