package com.greencom.android.podcasts.ui.explore

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * TODO: Documentation
 */
class ExplorePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    /** List of the genre names used for genre-specific tabs in the TabLayout. */
    private val genreNames = listOf(
        "News",
        "Society & Culture",
        "Education",
        "Science",
        "Technology",
        "Business",
        "History",
        "Arts",
        "Sports",
        "Health & Fitness",
    )

    // The number of pages is equal to the number of genre-specific pages plus one
    // page represented by an instance of the ExplorePrimaryPageFragment.
    override fun getItemCount(): Int = genreNames.size + 1

    // If a page is the first one, create the instance of the ExplorePrimaryPageFragment.
    // Otherwise, create an instance of the ExploreSecondaryPageFragment with provided
    // parameter of the genre name associated with the page.
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            ExplorePrimaryPageFragment()
        } else {
            val genreName = genreNames[position - 1]
            ExploreSecondaryPageFragment.newInstance(genreName)
        }
    }
}