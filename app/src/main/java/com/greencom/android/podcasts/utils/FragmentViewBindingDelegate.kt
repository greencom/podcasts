package com.greencom.android.podcasts.utils

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.functions

/**
 * Type alias for a `ViewBinding.bind()` functional type that receives a [View]
 * and returns a [ViewBinding] instance.
 */
private typealias ViewBinder<T> = (View) -> T

/**
 * [ViewBinding] delegated property that DOES NOT use reflection under the hood and
 * requires passing the `MyViewBinding::bind` function as a parameter. The property also will
 * be automatically cleared on `viewLifecycleOwner.onDestroy()` callback.
 *
 * - Note: passing layout to the Fragment constructor is required:
 *
 * `class MyFragment : Fragment(R.layout.fragment_my)`
 *
 * - Example:
 *
 * `private val binding by viewBinding(MyViewBinding::bind)`
 */
fun <T : ViewBinding> Fragment.viewBinding(viewBinder: ViewBinder<T>): ReadOnlyProperty<Fragment, T> {
    return FragmentViewBindingDelegateNoReflection(viewBinder)
}

/**
 * [ViewBinding] delegated property that uses reflection under the hood. You can either
 * specify ViewBinding class as a type parameter or as a property type. The property also will
 * be automatically cleared on `viewLifecycleOwner.onDestroy()` callback. Consider using
 * no-reflection version [viewBinding] because of performance issues.
 *
 * - Note: passing layout to the Fragment constructor is required:
 *
 * `class MyFragment : Fragment(R.layout.fragment_my)`
 *
 * - Examples:
 *
 * `private val binding: MyViewBinding by viewBinding()`
 *
 * `private val binding by viewBinding<MyViewBinding>()`
 */
inline fun <reified T : ViewBinding> Fragment.viewBinding(): ReadOnlyProperty<Fragment, T> {
    return FragmentViewBindingDelegateReflection(T::class)
}

private class FragmentViewBindingDelegateNoReflection<T : ViewBinding>(
    private val viewBinder: ViewBinder<T>,
) : ReadOnlyProperty<Fragment, T> {

    private var viewBinding: T? = null
    private val lifecycleObserver = ViewBindingLifecycleObserver()

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        viewBinding?.let { return it }

        thisRef.viewLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        return viewBinder(thisRef.requireView()).also {
            viewBinding = it
        }
    }

    private inner class ViewBindingLifecycleObserver : DefaultLifecycleObserver {

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            viewBinding = null
            owner.lifecycle.removeObserver(this)
        }
    }
}

/**
 * [ViewBinding] delegated property that uses reflection under the hood. Use [viewBinding]
 * shortcut instead.
 */
class FragmentViewBindingDelegateReflection<T : ViewBinding>(
    private val viewBindingClass: KClass<T>,
) : ReadOnlyProperty<Fragment, T> {

    private var viewBinding: T? = null
    private val lifecycleObserver = ViewBindingLifecycleObserver()

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        viewBinding?.let { return it }

        thisRef.viewLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        @Suppress("UNCHECKED_CAST")
        val bind = viewBindingClass.functions.first { it.name == "bind" } as ViewBinder<T>
        return bind(thisRef.requireView()).also {
            viewBinding = it
        }
    }

    private inner class ViewBindingLifecycleObserver : DefaultLifecycleObserver {

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            viewBinding = null
            owner.lifecycle.removeObserver(this)
        }
    }
}