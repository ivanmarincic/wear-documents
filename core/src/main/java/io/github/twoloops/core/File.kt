package io.github.twoloops.core

import org.json.JSONObject
import java.io.Serializable


class File : Serializable {
    var type: Int = 0
    var path: String = ""
    var name: String = ""
        get() {
            return path.substringAfterLast("/", "")
        }

    fun toJsonObject(): JSONObject {
        return JSONObject("{'name':'$name', 'path':'$path', 'type':$type}")
    }

    fun fromJsonObject(jsonObject: JSONObject) {
        type = jsonObject.getInt("type")
        path = jsonObject.getString("path")
    }

    override fun equals(other: Any?): Boolean {
        val otherFile = (other as File)
        return otherFile.path == path && otherFile.type == type
    }

    override fun hashCode(): Int {
        val file = java.io.File(path)
        return file.hashCode()
    }

    companion object {
        const val FILE_TYPE_FOLDER = 1
        const val FILE_TYPE_PDF = 2
        const val FILE_TYPE_DOCUMENT = 3
        const val FILE_TYPE_IMAGE = 4
    }
}