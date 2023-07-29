package de.tub.affinity3.android.persistence.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import de.tub.affinity3.android.classes.data.User
import io.reactivex.Flowable

@Dao
interface UserDao : BaseDao<User> {

    @Query("SELECT * FROM user")
    fun findAll(): Flowable<List<User>>

    @Query("SELECT * FROM user WHERE id = :id")
    fun findUserById(id: String): Flowable<User>

    @Query("SELECT * FROM user WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): LiveData<List<User>>
}
