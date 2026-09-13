package com.nihontabi.android.ui.nav

/** Route strings for the top-level [androidx.navigation.NavHost]. */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val MAP_COUNTRY = "map"
    const val MAP_PREFECTURE = "map/{prefectureId}"
    fun mapPrefecture(prefectureId: Int) = "map/$prefectureId"

    const val VISITS = "visits"
}
