package de.tub.affinity3.android.services

import android.content.Context
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.classes.data.Recommendation
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.repositories.RecommendationRepository
import de.tub.affinity3.android.util.getDeviceId
import io.reactivex.Observable
import timber.log.Timber

/**
 * This recommendation service implementation will be used as dummy implementation.
 */
class DummyRecommendationService(context: Context) : RecommendationService() {

    private val ratingRepository: RatingRepository = RatingRepository(context)

    private val deviceId: String = getDeviceId(context)

    private val recommendationRepository: RecommendationRepository =
        RecommendationRepository(context)

    override fun recommend(): Observable<List<Recommendation>> = Observable.create { _ ->
        Timber.i("Start new services")
        ratingRepository.getAllFromNearbyUsers(deviceId)
            .toObservable()
            .subscribe { ratings ->
                Timber.i("Recommendation Service detected ${ratings.size} new ratings from nearby users")
                Timber.i("Calculating recommendations ...")
                val newRecommendations = calculateRecommendations(ratings)
                Timber.i("Adding ${newRecommendations.size} new recommendations")
                Timber.i("Calculated $newRecommendations ...")

                recommendationRepository.addRecommendations(*newRecommendations.toTypedArray())
                    .subscribe()
            }
    }
}

fun calculateRecommendations(ratings: List<Rating>) =
    getRecommendations(
        ratings
            .filter { it.score > 3 }
            .map { rating -> rating.movieId }
    )

fun getRecommendations(movieIds: List<String>): List<Recommendation> {
    return movieIds.map { Recommendation(movieId = it, score = 10.0f) }
}
