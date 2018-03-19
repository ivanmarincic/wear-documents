package io.github.twoloops.weardocuments.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.gms.wearable.Node
import io.github.twoloops.weardocuments.R

class NodeListAdapter : RecyclerView.Adapter<NodeListAdapter.ViewHolder>() {

    var itemList: ArrayList<Node>? = null

    var listener: ((item: Node) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.node_picker_dialog_item, parent, false))
    }

    override fun getItemCount(): Int {
        return itemList!!.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList!![position]
        holder.textView.text = item.displayName
        holder.parent.setOnClickListener({
            listener?.invoke(item)
        })
    }

    inner class ViewHolder(var parent: View) : RecyclerView.ViewHolder(parent) {
        var textView: TextView = parent.findViewById(R.id.node_picker_dialog_item_text)
    }
}