package com.greencom.android.podcasts.ui.activity

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.ui.activity.bookmarks.ActivityBookmarksFragment
import com.greencom.android.podcasts.ui.activity.history.ActivityHistoryFragment
import com.greencom.android.podcasts.ui.activity.inprogress.ActivityInProgressFragment

/**
 * Adapter used for the ViewPager2 implementation inside [ActivityFragment]. Creates
 * [ActivityBookmarksFragment], [ActivityInProgressFragment] and [ActivityHistoryFragment].
 */
class ActivityViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val itemCount = fragment.resources.getStringArray(R.array.activity_tabs).size

    override fun getItemCount(): Int = itemCount

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ActivityBookmarksFragment()
            1 -> ActivityInProgressFragment()
            2 -> ActivityHistoryFragment()
            else -> throw IllegalStateException(
                "Fragment has not been added to ActivityViewPagerAdapter.createFragment()"
            )
        }
    }
}