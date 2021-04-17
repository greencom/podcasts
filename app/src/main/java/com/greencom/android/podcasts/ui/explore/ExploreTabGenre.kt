package com.greencom.android.podcasts.ui.explore

/**
 * Enum class that contains information about genres used as tabs in the [ExploreFragment].
 * All genre IDs, except [MAIN], correspond to data from ListenAPI. Podcasts with `genreId`
 * property set to `0` are predefined by the app.
 *
 * Note: the genre order is related to the tab order inside [ExploreFragment], so the order
 * of genres must match the order of the items in the `explore_tabs` <string-array>
 * declared in the `strings.xml` file. So do not forget to update `explore_tabs`
 * if you edit this class.
 */
enum class ExploreTabGenre(val id: Int) {
    MAIN(67),
    NEWS(99),
    SOCIETY_AND_CULTURE(122),
    EDUCATION(111),
    SCIENCE(107),
    TECHNOLOGY(127),
    BUSINESS(93),
    HISTORY(125),
    ARTS(100),
    SPORTS(77),
    HEALTH_AND_FITNESS(88),
}