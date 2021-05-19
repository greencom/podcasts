package com.greencom.android.podcasts.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * CustomDividerItemDecoration is a [DividerItemDecoration], but does not draw the divider
 * after the last element. Additionally, you can specify the number of dividers that this
 * DividerItemDecoration will skip at the top and bottom of the list using
 * [numberToSkipAtStart] and [numberToSkipAtEnd], which are default to 0.
 *
 * Supports both [RecyclerView.VERTICAL] and [RecyclerView.HORIZONTAL] orientations.
 * Defaults to [RecyclerView.VERTICAL].
 *
 * Use [setDrawable] for setting the [Drawable] for the divider.
 */
class CustomDividerItemDecoration(
    context: Context,
    private val orientation: Int = RecyclerView.VERTICAL,
    private val numberToSkipAtStart: Int = 0,
    private val numberToSkipAtEnd: Int = 0
) : DividerItemDecoration(context, orientation) {

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
        for (i in numberToSkipAtStart until childCount - (numberToSkipAtEnd + 1)) {
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
        for (i in numberToSkipAtStart until childCount - (numberToSkipAtEnd + 1)) {
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