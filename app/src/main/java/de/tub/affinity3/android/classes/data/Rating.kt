package de.tub.affinity3.android.classes.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.tub.affinity3.android.classes.data.Coordinates
import java.util.*

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
@Entity
data class Rating(
    /**
     * Ratings are abstractions of a user's preference for a movie.
     * Ratings have a rating <value> that elicits the preference by some user <userId>
     * on some movie <movieId>.
     *
     * A rating's metadata include the <date> and the <coordinates>. That is,
     * WHEN a rating has been received from either the device user or another user, or
     * WHERE a rating has been received from either the device user or another user.
     */
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "movieId") val movieId: String,
    @ColumnInfo(name = "userId") var userId: String,
    @ColumnInfo(name = "value") var score: Float,
    // metadata
    @ColumnInfo(name = "date") var date: Date,
    @Embedded var coordinates: Coordinates? = null
)

