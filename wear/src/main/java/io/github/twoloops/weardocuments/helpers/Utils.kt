package io.github.twoloops.weardocuments.helpers

import android.graphics.Bitmap
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
    }
}