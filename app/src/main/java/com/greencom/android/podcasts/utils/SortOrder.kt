package com.greencom.android.podcasts.utils

/**
 * Enum class that represents a sort order. Enum [value]s can be used in ListenApiService
 * methods.
 */
enum class SortOrder(val value: String) {
    /** Contains "recent_first" value. */
    RECENT_FIRST("recent_first"),

    /** Contains "oldest_first" value. */
    OLDEST_FIRST("oldest_first");

    /** Reverse [SortOrder] value. */
    fun reverse(): SortOrder = if (this == RECENT_FIRST) OLDEST_FIRST else RECENT_FIRST
}