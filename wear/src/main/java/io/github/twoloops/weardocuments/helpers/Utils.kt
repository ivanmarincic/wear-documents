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

import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import io.github.twoloops.core.File
import io.github.twoloops.weardocuments.R


class Utils {
    companion object {
        fun getIconForType(type: Int): Int {
            return when (type) {
                File.FILE_TYPE_IMAGE -> R.drawable.ic_image
                File.FILE_TYPE_PDF -> R.drawable.ic_pdf
                File.FILE_TYPE_DOCUMENT -> R.drawable.ic_document
                File.FILE_TYPE_FOLDER -> R.drawable.ic_folder
                else -> 0
            }
        }

        fun drawableInvertColors(drawable: Drawable): Drawable {
            val NEGATIVE = floatArrayOf(-1.0f, 0f, 0f, 0f, 255f, // red
                    0f, -1.0f, 0f, 0f, 255f, // green
                    0f, 0f, -1.0f, 0f, 255f, // blue
                    0f, 0f, 0f, 1.0f, 0f  // alpha
            )

            drawable.colorFilter = ColorMatrixColorFilter(NEGATIVE)
            return drawable
        }
    }
}