package com.greencom.android.podcasts.utils.extensions

import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.greencom.android.podcasts.R

/**
 * Set up [MaterialSharedAxis] transitions along a given axis for this fragment.
 * Axis defaults to [MaterialSharedAxis.Z].
 *
 * Use [enter], [exit], [popEnter], [popExit] parameters to specify which transitions should be
 * set up. These parameters default to `false`.
 */
fun Fragment.setupMaterialSharedAxisTransitions(
    axis: Int = MaterialSharedAxis.Z,
    enter: Boolean = false,
    exit: Boolean = false,
    popEnter: Boolean = false,
    popExit: Boolean = false,
) {
    val transition: (Boolean) -> MaterialSharedAxis = { forward ->
        MaterialSharedAxis(axis, forward).apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    if (enter) enterTransition = transition(true)
    if (exit) exitTransition = transition(true)
    if (popEnter) reenterTransition = transition(false)
    if (popExit) returnTransition = transition(false)
}

/**
 * Set up [MaterialSharedAxis] transitions along a given axis for this fragment.
 * Axis defaults to [MaterialSharedAxis.Z]. This method sets transitions for all directions,
 * use overloaded version of [setupMaterialSharedAxisTransitions] to be able to specify
 * certain directions only.
 */
fun Fragment.setupMaterialSharedAxisTransitions(axis: Int = MaterialSharedAxis.Z) {
    val transition: (Boolean) -> MaterialSharedAxis = { forward ->
        MaterialSharedAxis(axis, forward).apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    enterTransition = transition(true)
    exitTransition = transition(true)
    reenterTransition = transition(false)
    returnTransition = transition(false)
}

/**
 * Set up [MaterialFadeThrough] transitions for this fragment.
 *
 * Use [enter], [exit], [popEnter], [popExit] parameters to specify which transitions should be
 * set up. These parameters default to `false`.
 */
fun Fragment.setupMaterialFadeThroughTransitions(
    enter: Boolean = false,
    exit: Boolean = false,
    popEnter: Boolean = false,
    popExit: Boolean = false,
) {
    val transition: () -> MaterialFadeThrough = {
        MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    if (enter) enterTransition = transition()
    if (exit) exitTransition = transition()
    if (popEnter) reenterTransition = transition()
    if (popExit) returnTransition = transition()
}

/**
 * Set up [MaterialFadeThrough] transitions for this fragment.
 * This method sets transitions for all directions, use overloaded version of
 * [setupMaterialFadeThroughTransitions] to be able to specify certain directions only.
 */
fun Fragment.setupMaterialFadeThroughTransitions() {
    val transition: () -> MaterialFadeThrough = {
        MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    enterTransition = transition()
    exitTransition = transition()
    reenterTransition = transition()
    returnTransition = transition()
}