package de.tub.affinity3.android.repositories

import android.content.Context
import de.tub.affinity3.android.persistence.AffinityDatabase
import de.tub.affinity3.android.classes.data.Recommendation
import de.tub.affinity3.android.classes.data.Recommendation.Companion.FEEDBACK_EMPTY
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * This repository will be used as @single truth for accessing movies.
 */
class RecommendationRepository(val context: Context) {

    private val recommendationDao = AffinityDatabase.getInstance(context).recommendationDao()

    /**
     * Adds the [recommendations] to the database.
     */
    fun addRecommendations(vararg recommendations: Recommendation): Completable {
        return Completable.create { observable ->
            recommendations.forEach { recommendation ->
                Timber.d("Adding recommendation for movie with id ${recommendation.movieId} to database..")
            }
            recommendationDao.insert(*recommendations)
            observable.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    /**
     * Fetches all available [Recommendation]s.
     *
     * @return [Flowable] of a [List] of [Recommendation] objects.
     */
    fun findAllNew(): Flowable<List<Recommendation>> {
        Timber.d("Fetching all new recommendations")
        return recommendationDao.findAllWithFeedback(feedback = FEEDBACK_EMPTY)
                .subscribeOn(Schedulers.io())
    }

    /**
     * Updates [recommendation] in database.
     */
    fun updateRecommendation(recommendation: Recommendation): Completable {
        return Completable.create {
            recommendationDao.update(recommendation)
            it.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun countNew(): Observable<Int> {
        val counter = recommendationDao.count(feedback = FEEDBACK_EMPTY).subscribeOn(Schedulers.io())

        Timber.d("Counted $counter recommendations with FEEDBACK_EMPTY")
        return recommendationDao.count(feedback = FEEDBACK_EMPTY)
                .subscribeOn(Schedulers.io())
    }
}
