package de.tub.affinity3.android.repositories

import android.content.Context
import de.tub.affinity3.android.persistence.AffinityDatabase
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.util.getDeviceId
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * This is the single truth for accessing [Rating]
 */
class RatingRepository(val context: Context) {

    private val ratingDao = AffinityDatabase.getInstance(context).ratingDao()

    /**
     * Returns all mined [Rating]s for the given [movieIds].
     */
    fun getByMovieId(vararg movieIds: String): Flowable<List<Rating>> {
        return ratingDao.loadAllByMovieIds(*movieIds)
                .subscribeOn(Schedulers.io())
    }

    /**
     * Adds the [ratings] to the database.
     */
    fun addRating(vararg ratings: Rating): Completable {
        return Completable.create {
            ratings.forEach {
                Timber.d("Adding rating $it to database..")
            }
            ratingDao.insert(*ratings)

            it.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    /**
     * Updates [rating] in database.
     */
    fun updateRating(rating: Rating): Completable {
        return Completable.create {
            ratingDao.update(rating)
            it.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun getAllRatings(): Flowable<List<Rating>> {
        return ratingDao.getAll()
                .subscribeOn(Schedulers.io())
    }

    fun getAllFromNearbyUsers(deviceId: String): Flowable<List<Rating>> {
        return ratingDao.getAllFromNearbyUsers(deviceId)
                .subscribeOn(Schedulers.io())
    }

    fun getOwnRatings(): Flowable<List<Rating>> {
        val id = getDeviceId(context)
        return ratingDao.getOwnRatings(id)
                .subscribeOn(Schedulers.io())
    }

    fun deleteRatings(vararg ratings: Rating): Completable {
        return Completable.create {
            ratingDao.delete(*ratings)
            it.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun countOwnRatings(): Observable<Int> {
        return ratingDao.countRatingsByUserId(getDeviceId(context))
            .subscribeOn(Schedulers.io())
    }

    fun countExternalRatings(): Observable<Int> {
        return ratingDao.countExternalRatings(getDeviceId(context))
            .subscribeOn(Schedulers.io())
    }
}
