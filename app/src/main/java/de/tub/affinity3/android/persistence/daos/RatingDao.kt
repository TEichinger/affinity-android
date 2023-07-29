package de.tub.affinity3.android.persistence.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.tub.affinity3.android.classes.data.Rating
import io.reactivex.Flowable
import io.reactivex.Observable

@Dao
interface RatingDao : BaseDao<Rating> {

    @Query("SELECT * FROM rating")
    fun getAll(): Flowable<List<Rating>>

    @Query("SELECT * FROM rating WHERE userId = :userId")
    fun getOwnRatings(userId: String): Flowable<List<Rating>>

    @Query("SELECT * FROM rating WHERE userId != :deviceId")
    fun getAllFromNearbyUsers(deviceId: String): Flowable<List<Rating>>

    @Query("SELECT * FROM rating WHERE movieId IN (:movieIds)")
    fun loadAllByMovieIds(vararg movieIds: String): Flowable<List<Rating>>

    @Query("SELECT * FROM rating WHERE userId IN (:userIds)")
    fun loadAllByUserIds(userIds: IntArray): LiveData<List<Rating>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override fun insert(vararg obj: Rating)

    @Query("SELECT COUNT(*) FROM rating WHERE userId = :deviceId")
    fun countRatingsByUserId(deviceId: String): Observable<Int>

    @Query("SELECT COUNT(*) FROM rating WHERE userId != :deviceId")
    fun countExternalRatings(deviceId: String): Observable<Int>
}
