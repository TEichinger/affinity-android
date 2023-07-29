package de.tub.affinity3.android.persistence.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.tub.affinity3.android.classes.data.Recommendation
import io.reactivex.Flowable
import io.reactivex.Observable

@Dao
interface RecommendationDao : BaseDao<Recommendation> {

    @Query("SELECT * FROM recommendation")
    fun findAll(): Flowable<List<Recommendation>>

    @Query("SELECT * FROM recommendation WHERE feedback = :feedback")
    fun findAllWithFeedback(feedback: String): Flowable<List<Recommendation>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override fun insert(vararg obj: Recommendation)

    @Query("SELECT COUNT(*) FROM recommendation WHERE feedback = :feedback AND LENGTH(movieId) == 9")
    fun count(feedback: String): Observable<Int>
}
