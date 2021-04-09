package com.greencom.android.podcasts.ui.explore

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for the ViewPager2 implementation inside [ExploreFragment].
 * Creates the genre-specific [ExplorePageFragment] for every page.
 *
 * To change the order and/or genres for the pages edit the [ExploreTabGenre] enum class
 * and the `explore_tabs` <string-array> in the `strings.xml` file.
 */
class ExploreViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = ExploreTabGenre.values().size

    override fun createFragment(position: Int): Fragment {
        val genres = ExploreTabGenre.values()
        val genreId = genres[position].id
        return ExplorePageFragment.newInstance(genreId)
    }
}