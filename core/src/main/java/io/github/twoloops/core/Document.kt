package io.github.twoloops.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList


class Document : Serializable {

    var id: String = UUID.randomUUID().toString()
    var data: ArrayList<SerializableBitmap> = ArrayList()
    var dataCount = 0
    var dataChunkSize = 0
    var dataChunkStart = 0
    var imageFiles: ArrayList<java.io.File> = ArrayList()
    var files: ArrayList<File> = ArrayList()
    var type: Int = 0
    var name: String = ""

    fun toJsonObject(): JSONObject {
        val jsonArray = JSONArray()
        for (file in files) {
            jsonArray.put(file.toJsonObject())
        }
        return JSONObject("{'id': '$id', 'name':'$name', 'type':$type, 'files': $jsonArray, 'dataCount': $dataCount}")
    }

    fun fromJSONObject(json: JSONObject) {
        id = json.getString("id")
        type = json.getInt("type")
        name = json.getString("name")
        dataCount = json.getInt("dataCount")
//        dataChunkSize = json.getInt("dataChunkSize")
//        dataChunkStart = json.getInt("dataChunkStart")
        files = ArrayList()
        val dataJson = json.getJSONArray("files")
        for (i in 0..(dataJson.length() - 1)) {
            val file = File()
            file.fromJsonObject(dataJson.getJSONObject(i))
            files.add(file)
        }
    }
}