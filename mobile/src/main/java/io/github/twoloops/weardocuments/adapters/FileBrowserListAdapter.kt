package io.github.twoloops.weardocuments.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import io.github.twoloops.core.File
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.helpers.Utils

class FileBrowserListAdapter : RecyclerView.Adapter<FileBrowserListAdapter.ViewHolder>() {

    private val ITEM_TYPE_BACK_BUTTON = 0
    private val ITEM_TYPE_FILE = 1

    var itemList: ArrayList<File>? = null
    var selectedItems: HashMap<Int, File> = HashMap()
    var currentSelectionType: Int = 0

    var listener: ((item: File?) -> Unit)? = null
    var isRoot: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.file_browser_dialog_item, parent, false))
    }

    override fun getItemCount(): Int {
        return itemList!!.count() + if (isRoot) {
            0
        } else {
            1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && isRoot) {
            ITEM_TYPE_BACK_BUTTON
        } else {
            ITEM_TYPE_FILE
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = if (isRoot) {
            itemList!![position]
        } else {
            if (position > 0) {
                itemList!![position - 1]
            } else {
                null
            }
        }
        if (!isRoot && position == 0) {
            holder.textView.text = "..."
            holder.checkboxView.visibility = View.INVISIBLE
            holder.iconView.setImageResource(R.drawable.ic_folder)
            holder.parent.setOnClickListener {
                listener?.invoke(item)
            }
        } else {
            holder.textView.text = item!!.name
            holder.iconView.setImageResource(Utils.getIconForType(item.type))
            if (item.type == File.FILE_TYPE_FOLDER) {
                holder.checkboxView.visibility = View.INVISIBLE
                holder.parent.setOnClickListener {
                    listener?.invoke(item)
                }
            } else {
                holder.parent.setOnClickListener {
                    tryCheck(holder.checkboxView, item)
                }
                holder.checkboxView.visibility = View.VISIBLE
                holder.checkboxView.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        tryCheck(v as CheckBox, item)
                        true
                    } else {
                        false
                    }
                }
                holder.checkboxView.isChecked = selectedItems[item.hashCode()] != null
            }
        }
    }

    private fun tryCheck(view: CheckBox, item: File) {
        val count = selectedItems.count()
        if (count == 0) {
            currentSelectionType = item.type
        }
        if (currentSelectionType == item.type) {
            if (currentSelectionType == File.FILE_TYPE_IMAGE || count == 0 || (count == 1 && view.isChecked)) {
                view.toggle()
                if (selectedItems.remove(item.hashCode()) == null && view.isChecked) {
                    selectedItems[item.hashCode()] = item
                }
                listener?.invoke(item)
            }
        }
    }

    inner class ViewHolder(var parent: View) : RecyclerView.ViewHolder(parent) {
        var textView: TextView = parent.findViewById(R.id.file_browser_dialog_item_text)
        var iconView: ImageView = parent.findViewById(R.id.file_browser_dialog_item_icon)
        var checkboxView: CheckBox = parent.findViewById(R.id.file_browser_dialog_item_checkbox)
    }
}