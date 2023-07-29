package de.tub.affinity3.android.persistence.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
interface BaseDao<in T> {

    /**
     * Insert objects in the database.
     *
     * @param obj the objects to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg obj: T)

    /**
     * Update objects from the database.
     *
     * @param obj the objects to be updated
     */
    @Update
    fun update(vararg obj: T)

    /**
     * Delete objects from the database
     *
     * @param obj the objects to be deleted
     */
    @Delete
    fun delete(vararg obj: T)
}
