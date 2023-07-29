package de.tub.affinity3.android.persistence.daos

import androidx.room.Dao
import androidx.room.Query
import de.tub.affinity3.android.classes.data.CircularRegion
import io.reactivex.Flowable

@Dao
interface CircularRegionDao : BaseDao<CircularRegion> {

    @Query("SELECT * FROM circularRegion")
    fun findAll(): List<CircularRegion>

    @Query("SELECT * FROM circularRegion WHERE id = :id")
    fun findCircularRegionById(id: String): CircularRegion

}
