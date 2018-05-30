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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.contracts.DocumentAdapter
import io.github.twoloops.weardocuments.helpers.Utils
import java.io.File
import java.util.*


class DocumentViewAdapter(var context: Context) : DocumentAdapter {

    private var _document: Document? = null
    override var document: Document
        get() {
            return _document!!
        }
        set(value) {
            _document = value
            documentChangeListener?.invoke()
        }
    private var _currentPage: Int? = null
    override var currentPage: Int
        get() {
            return _currentPage!!
        }
        set(value) {
            _currentPage = value
            pageChangeListener?.invoke(value)
        }
    override var dark: Boolean = false
    override var zoomLevels: Int = 2
    override var zoomStrength: Float = 0.5f
    private var pageChangeListener: ((Int) -> Unit)? = null
    private var pageOnChangedListener: ((Int) -> Unit)? = null
    private var documentChangeListener: (() -> Unit)? = null
    private val targets: SparseArray<SimpleTarget<Drawable>> = SparseArray()

    override fun getItem(position: Int): File {
        return document.imageFiles[position]
    }

    override fun getCount(): Int {
        return document.imageFiles.count()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: ImageView = if (convertView == null) {
            val imageView = ImageView(parent.context)
            imageView.tag = UUID.randomUUID().toString()
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            imageView
        } else {
            val imageView = convertView as ImageView
            imageView.setImageDrawable(ColorDrawable(Color.DKGRAY))
            imageView
        }
        val target = object : SimpleTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                if (dark) {
                    view.setImageDrawable(Utils.drawableInvertColors(resource))
                } else {
                    view.setImageDrawable(resource)
                }
            }
        }
        targets.put(position, target)
        Glide.with(context)
                .load(getItem(position))
                .into(target)
        return view
    }

    override fun cancel(position: Int) {
        Glide.with(context).clear(targets[position])
    }

    override fun onPageChanged(page: Int) {
        _currentPage = page
        pageOnChangedListener?.invoke(page)
    }

    override fun setOnDocumentChangeListener(listener: () -> Unit) {
        documentChangeListener = listener
    }

    override fun setPageChangeListener(listener: (Int) -> Unit) {
        pageChangeListener = listener
    }

    override fun setOnPageChangedListener(listener: (Int) -> Unit) {
        pageOnChangedListener = listener
    }

    override fun refresh() {
        pageChangeListener?.invoke(currentPage)
    }
}