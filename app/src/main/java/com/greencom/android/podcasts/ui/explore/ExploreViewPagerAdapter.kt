package com.greencom.android.podcasts.ui.explore

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for the ViewPager2 implementation inside [ExploreFragment].
 * Creates the [ExplorePrimaryPageFragment] for the first page and
 * genre-specific [ExploreSecondaryPageFragment] for every other page.
 *
 * To change the order and/or genres for the pages edit the [ExploreTabGenre] enum class.
 * Note that the map must match the TabLayout TabConfigurationStrategy
 * inside the [ExploreFragment].
 */
class ExploreViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // The number of pages is equal to the number of genre-specific pages plus one
    // page represented by an instance of the ExplorePrimaryPageFragment.
    override fun getItemCount(): Int = ExploreTabGenre.values().size + 1

    // If a page is the first one, create the instance of the ExplorePrimaryPageFragment.
    // Otherwise, create an instance of the ExploreSecondaryPageFragment with provided
    // parameter of the genre id associated with the page.
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            ExplorePrimaryPageFragment()
        } else {
            val genres = ExploreTabGenre.values()
            val genreId = genres[position - 1].id
            ExploreSecondaryPageFragment.newInstance(genreId)
        }
    }
}