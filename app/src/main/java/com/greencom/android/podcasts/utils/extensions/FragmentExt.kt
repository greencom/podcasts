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
    fun createMaterialSharedAxisTransition(forward: Boolean): MaterialSharedAxis {
        return MaterialSharedAxis(axis, forward).apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    if (enter) enterTransition = createMaterialSharedAxisTransition(true)
    if (exit) exitTransition = createMaterialSharedAxisTransition(true)
    if (popEnter) reenterTransition = createMaterialSharedAxisTransition(false)
    if (popExit) returnTransition = createMaterialSharedAxisTransition(false)
}

/**
 * Set up [MaterialSharedAxis] transitions along a given axis for this fragment.
 * Axis defaults to [MaterialSharedAxis.Z]. This method sets transitions for all directions,
 * use overloaded version of [setupMaterialSharedAxisTransitions] to be able to specify
 * certain directions only.
 */
fun Fragment.setupMaterialSharedAxisTransitions(axis: Int = MaterialSharedAxis.Z) {
    fun createMaterialSharedAxisTransition(forward: Boolean): MaterialSharedAxis {
        return MaterialSharedAxis(axis, forward).apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    enterTransition = createMaterialSharedAxisTransition(true)
    exitTransition = createMaterialSharedAxisTransition(true)
    reenterTransition = createMaterialSharedAxisTransition(false)
    returnTransition = createMaterialSharedAxisTransition(false)
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
    fun createMaterialFadeThroughTransition(): MaterialFadeThrough {
        return MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    if (enter) enterTransition = createMaterialFadeThroughTransition()
    if (exit) exitTransition = createMaterialFadeThroughTransition()
    if (popEnter) reenterTransition = createMaterialFadeThroughTransition()
    if (popExit) returnTransition = createMaterialFadeThroughTransition()
}

/**
 * Set up [MaterialFadeThrough] transitions for this fragment.
 * This method sets transitions for all directions, use overloaded version of
 * [setupMaterialFadeThroughTransitions] to be able to specify certain directions only.
 */
fun Fragment.setupMaterialFadeThroughTransitions() {
    fun createMaterialFadeThroughTransition(): MaterialFadeThrough {
        return MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
        }
    }

    enterTransition = createMaterialFadeThroughTransition()
    exitTransition = createMaterialFadeThroughTransition()
    reenterTransition = createMaterialFadeThroughTransition()
    returnTransition = createMaterialFadeThroughTransition()
}