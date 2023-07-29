package de.tub.affinity3.android.util

import com.google.gson.annotations.SerializedName
import de.tub.affinity3.android.classes.data.Rating
import java.util.Calendar

data class PayloadRating(
    @SerializedName("score")
    val score: Float,
    @SerializedName("movieId")
    val movieId: String,
    @SerializedName("userId")
    val userId: String
) {
    fun toRating(): Rating {
        val date = Calendar.getInstance().time
        return Rating(id = userId + movieId, movieId = movieId, userId = userId, score = score, date = date)
    }
}
