package com.greencom.android.podcasts.utils.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

/** Get a [ColorStateList] by ID. */
fun Context.getColorStateListCompat(@ColorRes resId: Int): ColorStateList =
    AppCompatResources.getColorStateList(this, resId)

/** Get a [Drawable] by ID. */
fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable? =
    AppCompatResources.getDrawable(this, resId)