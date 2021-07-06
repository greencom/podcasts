package com.greencom.android.podcasts.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * CustomDividerItemDecoration behaves like a [DividerItemDecoration], but allows you
 * to specify whether to skip or not the first divider after the first item and the last
 * divider after the last item.
 *
 * Note: consider using default [DividerItemDecoration] with items which "android:layout_width"
 * and "android:layout_height" parameters are "match_parent" depending on the RecyclerView
 * orientation. Otherwise, the dividers may not be drawn correctly.
 *
 * It supports both [RecyclerView.VERTICAL] and [RecyclerView.HORIZONTAL] orientations.
 * Defaults to [RecyclerView.VERTICAL].
 *
 * Use [setDrawable] for setting the [Drawable] for the divider.
 */
class CustomDividerItemDecoration(
    context: Context,
    private val skipFirst: Boolean = false,
    private val skipLast: Boolean = true,
    private val orientation: Int = RecyclerView.VERTICAL,
) : DividerItemDecoration(context, orientation) {

    private val bounds = Rect()
    private var divider: Drawable? = null

    /** Sets the [Drawable] for this divider. */
    override fun setDrawable(drawable: Drawable) {
        divider = drawable
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || divider == null) return

        try {
            parent.layoutManager as LinearLayoutManager
        } catch (e: ClassCastException) {
            throw ClassCastException("CustomDividerItemDecoration can only be used with LinearLayoutManager")
        }

        when (orientation) {
            VERTICAL -> drawVertical(c, parent)
            HORIZONTAL -> drawHorizontal(c, parent)
            else -> throw IllegalArgumentException(
                "Invalid orientation. It should be either HORIZONTAL or VERTICAL"
            )
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        val mDivider = divider ?: return

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
        val start = if (skipFirst) 1 else 0
        val last = if (skipLast) 1 else 0

        if ((parent.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() <= start) {
            for (i in start until childCount - last) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val bottom = bounds.bottom + child.translationY.roundToInt()
                val top = bottom - mDivider.intrinsicHeight
                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(canvas)
            }
        } else {
            for (i in 0 until childCount - last) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val bottom = bounds.bottom + child.translationY.roundToInt()
                val top = bottom - mDivider.intrinsicHeight
                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(canvas)
            }
        }

        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        val mDivider = divider ?: return

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
        val start = if (skipFirst) 1 else 0
        val last = if (skipLast) 1 else 0

        if ((parent.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() <= start) {
            for (i in start until childCount - last) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val right = bounds.right + child.translationX.roundToInt()
                val left = right - mDivider.intrinsicWidth
                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(canvas)
            }
        } else {
            for (i in 0 until childCount - last) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val right = bounds.right + child.translationX.roundToInt()
                val left = right - mDivider.intrinsicWidth
                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(canvas)
            }
        }

        canvas.restore()
    }
}