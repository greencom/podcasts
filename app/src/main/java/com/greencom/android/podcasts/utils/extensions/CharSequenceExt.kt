package com.greencom.android.podcasts.utils.extensions

/** Returns `true` if the char sequence contains at least one HTML tag. */
fun CharSequence.containsHtmlTags(): Boolean = contains(Regex("<(\\w*|/\\w*)>"))