package io.github.twoloops.weardocuments.contracts

import android.database.DataSetObserver
import android.view.View
import android.view.ViewGroup
import java.io.File


interface DocumentLayoutAdapter {
    fun getView(position: Int, convertView: View?, parent: ViewGroup): View
    fun getItem(position: Int): File
    fun onPageChanged(page: Int)
    fun getCount(): Int
    fun getCurrentPage(): Int
    fun unregisterDataSetObserver(observer: DataSetObserver)
    fun registerDataSetObserver(observer: DataSetObserver)
    fun notifyDataSetChanged()
    fun setOnPageChangedListener(listener: (Int) -> Unit)
}