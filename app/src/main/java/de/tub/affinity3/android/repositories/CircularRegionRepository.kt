package de.tub.affinity3.android.repositories

import android.content.Context
import de.tub.affinity3.android.classes.data.CircularRegion
import de.tub.affinity3.android.persistence.AffinityDatabase
import de.tub.affinity3.android.classes.data.User
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers

/**
 * Repository to store [CircularRegion]s.
 */
class CircularRegionRepository(val context: Context) {

    private val circularRegionDao = AffinityDatabase.getInstance(context).circularRegionDao()

    /**
     * Returns all [User]s.
     */
    fun findAll(): List<CircularRegion> {
        return circularRegionDao.findAll()
    }

    /**
     * Returns [CircularRegion] by a given id
     */
    fun findCircularRegionById(circularRegionId: String): CircularRegion {
        return circularRegionDao.findCircularRegionById(circularRegionId)
    }

    fun addCircularRegion(circularRegion: CircularRegion): CircularRegion {
        circularRegionDao.insert(circularRegion)
        return findCircularRegionById(circularRegion.id)
    }

    fun deleteCircularRegion(circularRegion: CircularRegion) {
        circularRegionDao.delete(circularRegion)
    }

    fun updateCircularRegion(circularRegion: CircularRegion): CircularRegion {
        circularRegionDao.update(circularRegion)
        return findCircularRegionById(circularRegion.id)
    }

}
