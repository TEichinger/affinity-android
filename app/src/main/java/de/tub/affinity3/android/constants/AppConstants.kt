package de.tub.affinity3.android.constants

object AppConstants {
    const val IMDB_API_KEY_QUERY_PARAM = "apikey"
    const val FENCES_QUERY_PARAM = "fences"
    const val IMDB_API_BASE_URL = "http://www.omdbapi.com"
    const val IMDB_API_KEY = "d3bf0add"
    const val LEAFLET_HTML_PATH = "file:///android_asset/leaflet/index.html"
    const val LEAFLET_WEB_INTERFACE = "LeafletAndroid"
    const val USER_LOCATION_UPDATE_INTERVAL = 3000L
    const val USER_LOCATION_UPDATE_SHORTEST_INTERVAL = 1000L

    const val RECOMMENDATIONS_FLAG_EXTRA = "recommendations_flag"

    const val REGION_OF_INTEREST_RADIUS_METERS = 2500.0

    const val DBSCAN_EPS = 0.00001475962 * 50
    const val DBSCAN_MIN_POINTS = 50
}