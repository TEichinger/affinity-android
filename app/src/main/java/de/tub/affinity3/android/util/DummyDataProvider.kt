package de.tub.affinity3.android.util

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.tub.affinity3.android.classes.data.Movie
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.classes.data.Recommendation
import de.tub.affinity3.android.classes.data.User
import de.tub.affinity3.android.repositories.MovieRepository
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.repositories.RecommendationRepository
import de.tub.affinity3.android.repositories.UserRepository
import io.reactivex.Completable
import java.io.InputStreamReader
import java.util.Date
import java.util.UUID
import timber.log.Timber

object DummyDataProvider {

    fun addDummyData(context: Context) {
        Timber.d("Starting dummy data setup ...")

        addMovies(context)
//        addUser(context)
        addRecommendations(context)

        Timber.d("Dummy setup finished")
    }

    private fun addMovies(context: Context): List<Movie> {
        val movieFiles = context.assets.list("movies") ?: emptyArray()
        val newMovies: List<Movie> = movieFiles
            .map { movieFileName ->
                val jsonObject = parseJsonAsset(context, movieFileName)
                Movie.fromJSON(jsonObject)
            }

        val movieRepository = MovieRepository(context)
        val movieDeleteThrowable = movieRepository.deleteAll().blockingGet()
        Timber.e(movieDeleteThrowable)

        val movieAddThrowable = movieRepository.addMovie(*newMovies.toTypedArray()).blockingGet()
        Timber.e(movieAddThrowable)
        return newMovies
    }

    private fun addUser(context: Context) {
        val userRepository = UserRepository(context)
        val userAdd1Throwable =
            userRepository.addUser(User(id = "1", name = "Max Mustermann", lastSeen = Date()))
                .blockingGet()
        Timber.e(userAdd1Throwable)

        val userAdd2Throwable =
            userRepository.addUser(User(id = "2", name = "Tim Tom", lastSeen = Date()))
                .blockingGet()
        Timber.e(userAdd2Throwable)
    }

    private fun addRecommendations(context: Context) {
        val recommendationRepository = RecommendationRepository(context)
        val recommendations = arrayOf(
            Recommendation(movieId = "tt1345836", score = 5.0f)
        )

        val blocking = recommendationRepository.addRecommendations(*recommendations).blockingGet()
        Timber.e(blocking)
    }

    fun addRatings(
        context: Context,
        count: Int,
        useRandomScore: Boolean,
        external: Boolean
    ): Completable {
        // arbitrarily chosen movie id
        val startMovieId = 96891

        val deviceId = if (external) UUID.randomUUID().toString() else getDeviceId(context)
        val ratings = ArrayList<Rating>()
        for (i in 0 until count) {
            val ratingId = UUID.randomUUID().toString()
            val score: Float = if (useRandomScore) {
                (0..5).random().toFloat()
            } else {
                5f
            }

            val movieId = "tt00${startMovieId + i}"
            Timber.i("Adding rating with id $ratingId")
//            ratings.add(Rating(id = ratingId, movieId = movieId, userId = deviceId, score = score, date = Date()))
            ratings.add(
                Rating(
                    id = ratingId,
                    movieId = i.toString(),
                    userId = deviceId,
                    score = score,
                    date = Date()
                )
            )
        }

        val ratingsRepository = RatingRepository(context)
        return ratingsRepository.addRating(*ratings.toTypedArray())
    }

    private fun parseJsonAsset(context: Context, fileName: String): JsonObject {
        val inputStream = context.assets.open("movies/$fileName")
        return JsonParser.parseReader(
            InputStreamReader(inputStream, "UTF-8")
        ).asJsonObject
    }
}
