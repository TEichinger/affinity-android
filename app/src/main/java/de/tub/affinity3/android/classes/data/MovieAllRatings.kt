package de.tub.affinity3.android.classes.data

import androidx.room.Relation

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
data class MovieAllRatings(
    /**
     *
      */
    var imdbID: String,
    var title: String,
    var runtime: String?,
    var poster: String?,
    var genre: String,
    var isOnWatchlist: Boolean
) {
    @Relation(parentColumn = "imdbID", entityColumn = "movieId", entity = Rating::class)
    var ratings: List<Rating> = emptyList()
}
