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

package io.github.twoloops.weardocuments.adapters

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
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

    var itemList: List<File> = ArrayList()
    var selectedItems: SparseArray<File> = SparseArray()
    var currentSelectionType: Int = 0

    var listener: ((item: File?) -> Unit)? = null
    var isRoot: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.file_browser_dialog_item, parent, false))
    }

    override fun getItemCount(): Int {
        return itemList.count() + if (isRoot) {
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
            itemList[position]
        } else {
            if (position > 0) {
                itemList[position - 1]
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
        val count = selectedItems.size()
        if (count == 0) {
            currentSelectionType = item.type
        }
        if (currentSelectionType == item.type) {
            if (currentSelectionType == File.FILE_TYPE_IMAGE || count == 0 || (count == 1 && view.isChecked)) {
                view.toggle()
                val key = item.hashCode()
                val removedItem = selectedItems.get(key)
                if (removedItem == null && view.isChecked) {
                    selectedItems.put(key, item)
                } else {
                    selectedItems.remove(key)
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