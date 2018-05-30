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

package io.github.twoloops.weardocuments.contracts

import android.view.View
import android.view.ViewGroup
import io.github.twoloops.core.Document
import java.io.File


interface DocumentAdapter {
    var document: Document
    var currentPage: Int
    var dark: Boolean
    var zoomLevels: Int
    var zoomStrength: Float
    fun getView(position: Int, convertView: View?, parent: ViewGroup): View
    fun cancel(position: Int)
    fun getItem(position: Int): File
    fun onPageChanged(page: Int)
    fun getCount(): Int
    fun setOnDocumentChangeListener(listener: () -> Unit)
    fun setOnPageChangedListener(listener: (Int) -> Unit)
    fun setPageChangeListener(listener: (Int) -> Unit)
    fun refresh()
}