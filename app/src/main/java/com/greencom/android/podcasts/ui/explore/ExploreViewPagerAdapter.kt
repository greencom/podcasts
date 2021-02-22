package com.greencom.android.podcasts.ui.explore

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for the ViewPager2 implementation inside [ExploreFragment].
 * Creates the [ExplorePrimaryPageFragment] for the first page and
 * genre-specific [ExploreSecondaryPageFragment] for every other page.
 *
 * To change the order and/or genres for pages edit the [genres] map.
 * Note that the map must match the TabLayout TabConfigurationStrategy
 * inside the [ExploreFragment].
 */
class ExploreViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**
     * Map of the genres used for genre-specific tabs in the TabLayout.
     * `Key` is a genre name and `value` is a genre ID.
     */
    private val genres = mapOf(
        "News" to 99,
        "Society & Culture" to 122,
        "Education" to 111,
        "Science" to 107,
        "Technology" to 127,
        "Business" to 93,
        "History" to 125,
        "Arts" to 100,
        "Sports" to 77,
        "Health & Fitness" to 88,
    )

    // The number of pages is equal to the number of genre-specific pages plus one
    // page represented by an instance of the ExplorePrimaryPageFragment.
    override fun getItemCount(): Int = genres.size + 1

    // If a page is the first one, create the instance of the ExplorePrimaryPageFragment.
    // Otherwise, create an instance of the ExploreSecondaryPageFragment with provided
    // parameter of the genre id associated with the page.
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            ExplorePrimaryPageFragment()
        } else {
            val genreId = genres.values.toList()[position - 1]
            ExploreSecondaryPageFragment.newInstance(genreId)
        }
    }
}