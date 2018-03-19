package io.github.twoloops.weardocuments.adapters

import android.database.DataSetObserver
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.contracts.DocumentLayoutAdapter
import io.github.twoloops.weardocuments.tasks.ImageLoaderTask
import java.io.File
import java.util.*
import kotlin.collections.HashMap


class DocumentAdapter : DocumentLayoutAdapter {

    var document: Document? = null
    private var observer: DataSetObserver? = null
    private var tasks = HashMap<String, ImageLoaderTask>()
    var darkMode = false
    private var pageListener: ((Int) -> Unit)? = null

    override fun getItem(position: Int): File {
        return document!!.imageFiles[position]
    }

    override fun getCount(): Int {
        return document?.imageFiles?.count() ?: 0
    }

    override fun getCurrentPage(): Int {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: ImageView = if (convertView == null) {
            val imageView = ImageView(parent.context)
            imageView.tag = UUID.randomUUID().toString()
            imageView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            imageView
        } else {
            val imageView = convertView as ImageView
            imageView.setImageDrawable(ColorDrawable(Color.DKGRAY))
            tasks[imageView.tag]?.cancel(true)
            imageView
        }
        val task = ImageLoaderTask(getItem(position), darkMode)
        tasks[view.tag.toString()] = task
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, view)
        return view
    }


    override fun onPageChanged(page: Int) {
        pageListener?.invoke(page)
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        this.observer = observer
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver) {
        if (this.observer == observer) {
            this.observer = null
        }
    }

    override fun notifyDataSetChanged() {
        this.observer!!.onChanged()
    }

    override fun setOnPageChangedListener(listener: (Int) -> Unit) {
        pageListener = listener
    }
}