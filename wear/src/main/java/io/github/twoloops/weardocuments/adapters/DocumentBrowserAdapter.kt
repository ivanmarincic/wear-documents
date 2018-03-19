package io.github.twoloops.weardocuments.adapters

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.helpers.Utils
import org.json.JSONArray


class DocumentBrowserAdapter : RecyclerView.Adapter<DocumentBrowserAdapter.ViewHolder>() {

    var itemList: JSONArray? = null

    var listener: ((index: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.document_browser_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList!!.getJSONObject(position)
        holder.textView.text = item.getString("name")
        holder.iconView.setImageResource(Utils.getIconForType(item.getInt("type")))
        holder.iconView.setColorFilter(Color.WHITE)
        holder.parent.setOnClickListener({
            listener?.invoke(position)
        })
    }

    override fun getItemCount(): Int {
        return itemList!!.length()
    }

    inner class ViewHolder(var parent: View) : RecyclerView.ViewHolder(parent) {
        var textView: TextView = parent.findViewById(R.id.document_browser_item_text)
        var iconView: ImageView = parent.findViewById(R.id.document_browser_item_icon)
    }
}