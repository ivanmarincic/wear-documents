package io.github.twoloops.weardocuments.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.database.DataSetObserver
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.*
import android.widget.OverScroller
import io.github.twoloops.weardocuments.contracts.DocumentLayoutAdapter
import java.util.*
import kotlin.math.roundToInt


class DocumentLayout : ViewGroup {

    // TODO: Improve zooming
    // TODO: Rewrite layout
    private val factor = 0.146467f
    private var boxInset = 0
    private var isRound = false
    private var _adapter: DocumentLayoutAdapter? = null
    private var pageHeight = 0
    private var pageWidth = 0
    private var eventStartX = 0f
    private var eventStartY = 0f
    private var lastScrollX = 0
    private var maxScrollX = 0
    private var lastScrollY = 0
    private var maxScrollY = 0
    private var scale = 1f
    private var scaleLevels = 2
    private var currentScaleLevel = 1
    private var invalidChildren = LinkedList<Child>()
    private var loadedPages = TreeMap<Int, Child>()
    private var lastPage = 0
    private var cacheThresholdPage = 5
    private var _currentPage = 0
    private var hasBeenRefreshed = false
    var currentPage: Int
        get() {
            return _currentPage
        }
        set(value) {
            scrollToPage(value)
            refreshViewsFromAdapter()
        }
    var zoomLevels: Int
        get() {
            return scaleLevels
        }
        set(value) {
            scaleLevels = value
            requestLayout()
        }
    private var scaleStrength = 1f
    var zoomStrength: Float
        get() {
            return scaleStrength
        }
        set(value) {
            scaleStrength = value
            requestLayout()
        }
    private var zoomAnimator: ValueAnimator? = null
    private val scroller by lazy(LazyThreadSafetyMode.NONE) {
        OverScroller(context)
    }
    private val velocityTracker by lazy(LazyThreadSafetyMode.NONE) {
        VelocityTracker.obtain()
    }
    var adapter: DocumentLayoutAdapter
        get() = _adapter!!
        set(value) {
            if (_adapter != null) {
                _adapter!!.unregisterDataSetObserver(observer)
            }
            _adapter = value
            _adapter!!.registerDataSetObserver(observer)
            fillViewsFromAdapter()
        }
    private val observer = object : DataSetObserver() {

        override fun onChanged() {
            fillViewsFromAdapter()
        }

        override fun onInvalidated() {
            refreshViewsFromAdapter()
        }
    }
    private val onGestureListener by lazy(LazyThreadSafetyMode.NONE) {
        OnGestureListener()
    }
    private val gestureDetector by lazy(LazyThreadSafetyMode.NONE) {
        GestureDetector(context, onGestureListener)
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        boxInset = (factor * Math.max(Resources.getSystem().displayMetrics.heightPixels, Resources.getSystem().displayMetrics.widthPixels)).toInt()
        isRound = resources.configuration.isScreenRound
        if (!isRound) {
            boxInset = (5 * Resources.getSystem().displayMetrics.density).toInt()
        }
        pageHeight = Resources.getSystem().displayMetrics.heightPixels
        pageWidth = Resources.getSystem().displayMetrics.widthPixels
        gestureDetector.setOnDoubleTapListener(onGestureListener)
        pivotX = 0f
        pivotY = 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childWidth = pageWidth - (2 * boxInset)
        for (child in invalidChildren) {
            child.view.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(pageHeight, MeasureSpec.EXACTLY))
        }
        setMeasuredDimension(
                MeasureSpec.makeMeasureSpec(pageWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(_adapter!!.getCount() * pageHeight + (boxInset * 2), MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childWidth = pageWidth - (2 * boxInset)
        val left = boxInset
        val right = left + childWidth
        while (!invalidChildren.isEmpty()) {
            val child = invalidChildren.poll()
            if (loadedPages[child.index] == null) {
                val top = boxInset + (child.index * pageHeight)
                val bottom = top + pageHeight
                child.view.layout(left, top, right, bottom)
                loadedPages[child.index] = child
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                eventStartX = event.x
                eventStartY = event.y
                maxScrollX = (width - pageWidth / scale).roundToInt()
                maxScrollY = (height - pageHeight / scale).roundToInt()
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.computeCurrentVelocity(1000)
                scroller.fling(scrollX, scrollY, -velocityTracker.xVelocity.roundToInt(), -velocityTracker.yVelocity.roundToInt(), 0, maxScrollX, 0, maxScrollY)
                ViewCompat.postInvalidateOnAnimation(this)
                velocityTracker.clear()
                lastScrollX = scrollX
                lastScrollY = scrollY
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker.addMovement(event)
                var newScrollX = (lastScrollX + (eventStartX - event.x)).roundToInt()
                if (newScrollX < 0) {
                    newScrollX = 0
                }
                if (newScrollX > maxScrollX) {
                    newScrollX = maxScrollX
                }
                scrollX = newScrollX
                var newScrollY = (lastScrollY + (eventStartY - event.y)).roundToInt()
                if (newScrollY < 0) {
                    newScrollY = 0
                }
                if (newScrollY > maxScrollY) {
                    newScrollY = maxScrollY
                }
                scrollY = newScrollY
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val newScrollX = scroller.currX
            val newScrollY = scroller.currY
            if (newScrollX < maxScrollX) {
                scrollX = newScrollX
                lastScrollX = newScrollX
            }
            if (newScrollY < maxScrollY) {
                scrollY = newScrollY
                lastScrollY = newScrollY
            }
            ViewCompat.postInvalidateOnAnimation(this)
        }
        _currentPage = scrollY / pageHeight
        val pageChange = _currentPage - lastPage
        if (pageChange != 0) {
            onPageChanged(pageChange)
        }
    }

    private fun onPageChanged(pageChangeValue: Int) {
        if (!hasBeenRefreshed) {
            val currentPageWithOffset = _currentPage - Math.ceil((cacheThresholdPage / 2.0)).toInt()
            if (currentPageWithOffset in 0 until _adapter!!.getCount() - cacheThresholdPage) {
                if (pageChangeValue > 0) {
                    val child = loadedPages.pollFirstEntry()?.value
                    val newPosition = currentPageWithOffset + cacheThresholdPage
                    moveChild(_adapter!!.getView(newPosition, child!!.view, this), newPosition)
                } else {
                    val child = loadedPages.pollLastEntry()?.value
                    moveChild(_adapter!!.getView(currentPageWithOffset, child!!.view, this), currentPageWithOffset)
                }
                requestLayout()
            }
        }
        hasBeenRefreshed = false
        lastPage = _currentPage
        _adapter!!.onPageChanged(_currentPage)
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return currentScaleLevel > 1
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return childCount > 0
    }

    private fun zoomIn() {
        currentScaleLevel++
        animateZoom(scale, scale + Math.round(scaleStrength * 10.0f) / 10.0f)
    }

    private fun moveChild(view: View, position: Int) {
        val child = Child(view, position)
        if ((loadedPages[position] == null)) {
            if (invalidChildren.indexOf(child) == -1) {
                invalidChildren.add(child)
            }
        }
    }

    private fun scrollToPage(pageToScrollTo: Int) {
        _currentPage = pageToScrollTo
        lastScrollY = _currentPage * pageHeight
        scrollY = lastScrollY
    }

    private fun zoomOut() {
        val scaledScrollXMax = width - pageWidth
        val scaledScrollYMax = height - pageHeight
        val dx = if (scaledScrollXMax < scrollX) {
            scaledScrollXMax - scrollX
        } else {
            0
        }
        val dy = if (scaledScrollYMax < scrollY) {
            -scaledScrollYMax - scrollY
        } else {
            0
        }
        if (dx != 0 || dy != 0) {
            scrollBy(dx, dy)
            ViewCompat.postInvalidateOnAnimation(this)
        }
        currentScaleLevel = 1
        animateZoom(scale, 1f)
    }

    private fun animateZoom(oldValue: Float, newValue: Float) {
        if (zoomAnimator != null && zoomAnimator!!.isRunning) {
            clearAnimation()
        }
        zoomAnimator = ValueAnimator.ofFloat(oldValue, newValue)
        zoomAnimator!!.addUpdateListener {
            val currentValue = it.animatedValue as Float
            scaleX = currentValue
            scaleY = currentValue
            scale = currentValue
        }
        zoomAnimator!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        zoomAnimator!!.duration = 300
        zoomAnimator!!.start()
    }

    private fun refreshViewsFromAdapter() {
        loadedPages.clear()
        val currentPageWithOffset = if (_adapter!!.getCount() >= cacheThresholdPage) {
            Math.min(Math.max(_currentPage - (cacheThresholdPage / 2), 0), _adapter!!.getCount() - cacheThresholdPage)
        } else {
            0
        }
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val pageWithOffset = currentPageWithOffset + i
            moveChild(_adapter!!.getView(pageWithOffset, view, this), pageWithOffset)
        }
        requestLayout()
        hasBeenRefreshed = true
    }

    private fun fillViewsFromAdapter() {
        removeAllViews()
        loadedPages.clear()
        invalidChildren.clear()
        if (_adapter == null) return
        for (i in 0 until Math.min(_adapter!!.getCount(), cacheThresholdPage)) {
            val view = _adapter!!.getView(i, null, this)
            val child = Child(view, i)
            invalidChildren.add(child)
            addView(view)
        }
    }

    private inner class OnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            if (e != null) {
                if (scaleLevels + 1 == currentScaleLevel) {
                    zoomOut()
                } else {
                    zoomIn()
                }
            }
            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }
    }

    private inner class Child(var view: View, var index: Int)
}
