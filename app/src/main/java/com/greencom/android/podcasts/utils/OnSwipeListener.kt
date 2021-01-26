package com.greencom.android.podcasts.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

private const val SWIPE_DISTANCE_THRESHOLD = 75
private const val SWIPE_VELOCITY_THRESHOLD = 100

/**
 * Class definition for callbacks to be invoked when the system detects a swipe on this view.
 * There are four methods available for defining behavior for swipes in different
 * directions: [onSwipeLeft], [onSwipeRight], [onSwipeTop], [onSwipeBottom].
 */
open class OnSwipeListener(context: Context) : View.OnTouchListener {

    private val gestureDetector = GestureDetector(context, GestureListener())

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    /** Callback to be invoked when a view is swiped right. */
    open fun onSwipeRight() {}

    /** Callback to be invoked when a view is swiped left. */
    open fun onSwipeLeft() {}

    /** Callback to be invoked when a view is swiped top. */
    open fun onSwipeTop() {}

    /** Callback to be invoked when a view is swiped bottom. */
    open fun onSwipeBottom() {}

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
        ): Boolean {
            var result = false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_DISTANCE_THRESHOLD
                        && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    result = true
                }
            } else if (abs(diffY) > SWIPE_DISTANCE_THRESHOLD
                    && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeBottom()
                } else {
                    onSwipeTop()
                }
                result = true
            }

            return result
        }
    }
}
