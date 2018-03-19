package io.github.twoloops.core

import android.graphics.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.Serializable
import android.graphics.Bitmap




class SerializableBitmap(bitmap: Bitmap) : Serializable {

    var currentImage: Bitmap? = bitmap

    @Throws(IOException::class)
    private fun writeObject(out: java.io.ObjectOutputStream) {
        val stream = ByteArrayOutputStream()
        currentImage!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        out.writeInt(byteArray.size)
        out.write(byteArray)
        stream.close()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: java.io.ObjectInputStream) {
        try {
            val bufferLength = `in`.readInt()
            val byteArray = ByteArray(bufferLength)
            var pos = 0
            do {
                val read = `in`.read(byteArray, pos, bufferLength - pos)
                if (read != -1) {
                    pos += read
                } else {
                    break
                }
            } while (pos < bufferLength)
            currentImage = BitmapFactory.decodeByteArray(byteArray, 0, bufferLength)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun Bitmap.getInverted(): Bitmap {
    val invertedColorMatrix = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
    val sepiaColorFilter = ColorMatrixColorFilter(
            invertedColorMatrix)
    val bitmap = Bitmap.createBitmap(this.width, this.height,
            Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    paint.colorFilter = sepiaColorFilter
    canvas.drawBitmap(this, 0f, 0f, paint)
    this.recycle()
    return bitmap
}

fun Bitmap.getGreyscale(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.width, this.height,
            Bitmap.Config.RGB_565)
    val c = Canvas(bitmap)
    val paint = Paint()
    val cm = ColorMatrix()
    cm.setSaturation(0f)
    val f = ColorMatrixColorFilter(cm)
    paint.colorFilter = f
    c.drawBitmap(this, 0f, 0f, paint)
    return bitmap
}

fun Bitmap.getBrightness(): Int {
    var r = 0
    var g = 0
    var b = 0
    var n = 0
    val pixels = IntArray(width * height)
    this.getPixels(pixels, 0, width, 0, 0, width, height)
    for (i in 0..(pixels.size - 1)) {
        val color = pixels[i]
        r += Color.red(color)
        g += Color.green(color)
        b += Color.blue(color)
        n++
    }
    return (r + g + b) / (n * 3)
}

fun Bitmap.toBinary(): Bitmap {
    val threshold = 127
    val bmpBinary = Bitmap.createBitmap(this)

    for (x in 0 until width) {
        for (y in 0 until height) {
            // get one pixel color
            val pixel = this.getPixel(x, y)
            val red = Color.red(pixel)

            //get binary value
            if (red < threshold) {
                bmpBinary.setPixel(x, y, -0x1000000)
            } else {
                bmpBinary.setPixel(x, y, -0x1)
            }

        }
    }
    return bmpBinary
}