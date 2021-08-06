package com.greencom.android.podcasts.player

/**
 * Value used to skip backward for.
 *
 * Note: sign respects skip direction.
 */
const val PLAYER_SKIP_BACKWARD_VALUE = -10_000L

/**
 * Value used to skip forward for.
 *
 * Note: sign respects skip direction.
 */
const val PLAYER_SKIP_FORWARD_VALUE = 30_000L

/** Key to retrieve a duration from CustomCommand args to be set to a sleep timer. */
const val PLAYER_SET_SLEEP_TIMER = "PLAYER_SET_SLEEP_TIMER_VALUE"