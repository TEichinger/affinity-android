package de.tub.affinity3.android.classes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
@Entity
data class Recommendation(
    @PrimaryKey @ColumnInfo(name = "movieId") var movieId: String,
    @ColumnInfo(name = "score") var score: Float,
    @ColumnInfo(name = "feedback") var feedback: String = FEEDBACK_EMPTY
) {
    /**
     * Recommendations are based on the device user's collected ratings.
     * Recommendations are associated with a recommendation <score> and the device user's <feedback>.
     *
     * The <score> can be associated with an estimated rating of what rating the device user
     * would give a movie. The <score> determines (a) if, and (b) in which order recommendations
     * are proposed to the device user.
     *
     * The higher the <score>, the higher a movie will be placed in the stack of recommendation
     * cards (see ../views/RecommendationCard.kt).
     *
     */


    companion object {
        const val FEEDBACK_EMPTY = "EMPTY"
        const val FEEDBACK_KNOWN = "KNOWN"
        const val FEEDBACK_LIKE = "LIKE"
        const val FEEDBACK_DISLIKE = "DISLIKE"
    }
}
