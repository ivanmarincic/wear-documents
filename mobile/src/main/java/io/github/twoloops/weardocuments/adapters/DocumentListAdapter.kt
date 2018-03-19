package io.github.twoloops.weardocuments.adapters

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.helpers.Utils

class DocumentListAdapter : RecyclerView.Adapter<DocumentListAdapter.ViewHolder>() {

    var itemList: ArrayList<Document>? = null

    var listener: ((item: Document) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_view_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return itemList!!.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList!![position]
        holder.textView.text = item.name
        holder.iconView.setImageResource(Utils.getIconForType(item.type))
        holder.parent.setOnClickListener({
            listener?.invoke(item)
        })
    }

    inner class ViewHolder(var parent: View) : RecyclerView.ViewHolder(parent) {
        var textView: TextView = parent.findViewById(R.id.main_view_list_item_text)
        var iconView: ImageView = parent.findViewById(R.id.main_view_list_item_icon)
    }
}