package com.greencom.android.podcasts.ui.explore

/**
 * Enum class that contains information about genres used as tabs in the [ExploreFragment].
 * Genre IDs correspond to the information from ListenAPI.
 *
 * Note: the genre order is related to the tab order inside [ExploreFragment], so the order
 * of genres must match the order of items in the `explore_tabs` <string-array>
 * (except non-genre `Main`) declared in the strings.xml file. So do not forget to update
 * `explore_tabs` if you edit this class.
 */
enum class ExploreTabGenre(val id: Int) {
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