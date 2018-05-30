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

package io.github.twoloops.weardocuments.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.SparseArray
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import io.github.twoloops.weardocuments.contracts.DocumentAdapter
import java.util.*
import kotlin.math.roundToInt


class DocumentView : ViewGroup, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    var _adapter: DocumentAdapter? = null
    var adapter: DocumentAdapter
        get() {
            return _adapter!!
        }
        set(value) {
            _adapter = value
            setAdapterListeners()
        }

    private var pageCount = 0
    private var boxInset: Int = 0
    private var isRound: Boolean = false
    private var childHeight: Int = 0
    private var childWidth: Int = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private var zoomAnimator: Animator? = null
    private var scaleFactor = 1f
    private var touchEvent: MotionEvent? = null
    private var lastScrollX: Int = 0
    private var lastScrollY: Int = 0
    private var invalidChildren = ArrayDeque<QueueItem>()
    private var loadedChildren = SparseArray<Child>()
    private var currentPage = 0
    private var maxLoadedPages = 5
    private var currentZoomLevel = 0
    private var scrollMinY = 0
    private var scrollMaxY = 0
    private var scrollMinX = 0
    private var scrollMaxX = 0
    private val scroller by lazy {
        OverScroller(context)
    }
    private val zoomAnimationDuration by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime)
    }
    private val gestureDetector by lazy {
        GestureDetector(context, this)
    }

    init {
        boxInset = (0.146467f * Math.max(Resources.getSystem().displayMetrics.heightPixels, Resources.getSystem().displayMetrics.widthPixels)).toInt()
        isRound = resources.configuration.isScreenRound
        if (!isRound) {
            boxInset = (5 * Resources.getSystem().displayMetrics.density).toInt()
        }
        screenHeight = Resources.getSystem().displayMetrics.heightPixels
        childHeight = screenHeight - (boxInset * 2)
        screenWidth = Resources.getSystem().displayMetrics.widthPixels
        childWidth = screenWidth - (boxInset * 2)
        gestureDetector.setOnDoubleTapListener(this)
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        for (item in invalidChildren) {
            loadedChildren[item.indexToDelete].view.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY))
        }
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val iterator = invalidChildren.iterator()
        for (item in iterator) {
            if (invalidChildren.size <= maxLoadedPages) {
                val child = loadedChildren[item.indexToDelete]
                adapter.cancel(child.position)
                child.position = item.indexToLoad
                child.view = adapter.getView(item.indexToLoad, child.view, this)
                val top = item.indexToLoad * childHeight + boxInset
                val left = boxInset
                val right = left + childWidth
                val bottom = top + childHeight
                child.view.layout(left, top, right, bottom)
                loadedChildren.put(item.indexToLoad, child)
            }
            iterator.remove()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        val olcScaleFactor = scaleFactor
        if (currentZoomLevel < adapter.zoomLevels) {
            currentZoomLevel++
        } else {
            currentZoomLevel = 0
        }
        scaleFactor = 1f + (adapter.zoomStrength * currentZoomLevel)
        val newScrollX: Int
        val newScrollY: Int
        calculateScrollArea()
        if (currentZoomLevel == 0) {
            newScrollX = 0
            newScrollY = Math.max(scrollMinY, lastScrollY - (childHeight / 2))
        } else {
            val x = e.getX(0).toInt()
            val y = e.getY(0).toInt()
            newScrollX = Math.max(scrollMinX, Math.min(lastScrollX + (x - (screenWidth / 2)), scrollMaxX))
            newScrollY = Math.max(scrollMinY, Math.min(lastScrollY + (y - (screenHeight / 2)), scrollMaxY))
        }
        val set = AnimatorSet()
        set
                .play(ObjectAnimator.ofFloat(this, View.SCALE_X, olcScaleFactor, scaleFactor))
                .with(ObjectAnimator.ofFloat(this, View.SCALE_Y, olcScaleFactor, scaleFactor))
                .with(ObjectAnimator.ofInt(this, "scrollX", lastScrollX, newScrollX))
                .with(ObjectAnimator.ofInt(this, "scrollY", lastScrollY, newScrollY))
        set.duration = zoomAnimationDuration.toLong()
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                zoomAnimator = null
            }

            override fun onAnimationCancel(animation: Animator) {
                zoomAnimator = null
            }
        })
        set.start()
        zoomAnimator = set
        return true
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return scaleFactor > 1f
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent?): Boolean {
        touchEvent = e
        lastScrollX = scrollX
        lastScrollY = scrollY
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        scroller.fling(scrollX, scrollY, velocityX.roundToInt().times(-1), velocityY.roundToInt().times(-1), scrollMinX, scrollMaxX, scrollMinY, scrollMaxY)
        ViewCompat.postInvalidateOnAnimation(this)
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        scrollX = Math.max(scrollMinX, Math.min((scrollX + distanceX).roundToInt(), scrollMaxX))
        scrollY = Math.max(scrollMinY, Math.min((scrollY + distanceY).roundToInt(), scrollMaxY))
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val newScrollX = scroller.currX
            val newScrollY = scroller.currY
            scrollX = newScrollX
            lastScrollX = newScrollX
            scrollY = newScrollY
            lastScrollY = newScrollY
            ViewCompat.postInvalidateOnAnimation(this)
        }
        val pageDelta = scrollY / childHeight
        if (pageDelta != currentPage) {
            onPageChanged(pageDelta, currentPage)
            currentPage = pageDelta
        }
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    private fun onPageChanged(currentPage: Int, previousPage: Int) {
        val scrollDirection = currentPage - previousPage
        val pageToLoad = currentPage + (maxLoadedPages / 2) * scrollDirection
        val pageToDelete = previousPage + (maxLoadedPages / 2) * -scrollDirection
        if (pageToLoad in 0 until pageCount && pageToDelete in 0 until pageCount) {
            invalidChildren.add(QueueItem(pageToLoad, pageToDelete % maxLoadedPages))
            requestLayout()
        }
        adapter.onPageChanged(currentPage)
    }

    private fun setAdapterListeners() {
        adapter.setOnDocumentChangeListener {
            pageCount = adapter.getCount()
            insertViewsFromAdapter()
            calculateScrollArea()
        }
        adapter.setPageChangeListener {
            insertViewsFromAdapter(it)
            scrollY = currentPage * childHeight + boxInset
        }
    }

    private fun insertViewsFromAdapter(start: Int = 0) {
        removeAllViews()
        loadedChildren.clear()
        invalidChildren.clear()
        val startingIndex = if (start > (pageCount - (maxLoadedPages / 2))) {
            Math.max(0, pageCount - maxLoadedPages)
        } else {
            Math.max(0, start - (maxLoadedPages / 2))
        }
        for (i in startingIndex until Math.min(startingIndex + maxLoadedPages, pageCount)) {
            val view = adapter.getView(i, null, this)
            loadedChildren.put(i % maxLoadedPages, Child(view, i))
            invalidChildren.add(QueueItem(i, i % maxLoadedPages))
            addView(view)
        }
        currentPage = start
        adapter.onPageChanged(currentPage)
    }

    private fun calculateScrollArea() {
        val scaleOffsetX = ((childWidth - (childWidth / scaleFactor)) / 2).roundToInt()
        val scaleOffsetY = ((childHeight - (childHeight / scaleFactor)) / 2).roundToInt()
        scrollMaxX = scaleOffsetX
        scrollMinX = -scaleOffsetX
        scrollMaxY = (pageCount - 1) * childHeight + scaleOffsetY
        scrollMinY = 0 - scaleOffsetY
    }

    inner class Child(var view: View, var position: Int)
    inner class QueueItem(var indexToLoad: Int, var indexToDelete: Int)

}