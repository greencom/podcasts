package com.greencom.android.podcasts.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * CustomDividerItemDecoration behaves like a [DividerItemDecoration], but allows
 * you to specify how many dividers to skip at the beginning and end of the list, passing
 * the appropriate values to the [numberToSkipAtStart] and [numberToSkipAtEnd] parameters.
 * [numberToSkipAtStart] defaults to 0 and [numberToSkipAtEnd] defaults to 1 to skip the
 * divider after the last item of the list.
 *
 * It supports both [RecyclerView.VERTICAL] and [RecyclerView.HORIZONTAL] orientations.
 * Defaults to [RecyclerView.VERTICAL].
 *
 * Use [setDrawable] for setting the [Drawable] for the divider.
 */
class CustomDividerItemDecoration(
    context: Context,
    private val orientation: Int = RecyclerView.VERTICAL,
    private val numberToSkipAtStart: Int = 0,
    private val numberToSkipAtEnd: Int = 1
) : DividerItemDecoration(context, orientation) {

    init {
        // Check parameters.
        if (numberToSkipAtStart < 0 || numberToSkipAtEnd < 0) {
            throw IllegalArgumentException(
                "numberToSkipAtStart and numberToSkipAtEnd parameters must be at least 0"
            )
        }
    }

    private val bounds = Rect()
    private lateinit var divider: Drawable

    /** Sets the [Drawable] for this divider. */
    override fun setDrawable(drawable: Drawable) {
        divider = drawable
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null) return

        when (orientation) {
            VERTICAL -> drawVertical(c, parent)
            HORIZONTAL -> drawHorizontal(c, parent)
            else -> throw IllegalArgumentException(
                "Invalid orientation. It should be either HORIZONTAL or VERTICAL"
            )
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int

        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left,
                parent.paddingTop,
                right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }

        val childCount = parent.childCount
        for (i in numberToSkipAtStart until childCount - numberToSkipAtEnd) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val bottom = bounds.bottom + child.translationY.roundToInt()
            val top = bottom - divider.intrinsicHeight
            divider.setBounds(left, top, right, bottom)
            divider.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int

        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(
                parent.paddingLeft,
                top,
                parent.width - parent.paddingRight,
                bottom
            )
        } else {
            top = 0
            bottom = parent.height
        }

        val childCount = parent.childCount
        for (i in numberToSkipAtStart until childCount - numberToSkipAtEnd) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val right = bounds.right + child.translationX.roundToInt()
            val left = right - divider.intrinsicWidth
            divider.setBounds(left, top, right, bottom)
            divider.draw(canvas)
        }
        canvas.restore()
    }
}