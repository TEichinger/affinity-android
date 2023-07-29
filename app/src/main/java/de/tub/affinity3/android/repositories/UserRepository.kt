package de.tub.affinity3.android.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import de.tub.affinity3.android.persistence.AffinityDatabase
import de.tub.affinity3.android.classes.data.User
import de.tub.affinity3.android.util.UsernameGenerator
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Single truth for accessing [User]s.
 */
class UserRepository(val context: Context) {

    private val userDao = AffinityDatabase.getInstance(context).userDao()
    /**
     * Returns all [User]s.
     */
    fun findAll(): Flowable<List<User>> {
        return userDao.findAll()
                .subscribeOn(Schedulers.io())
    }

    /**
     * Returns [User] by a given id
     */
    fun findUserById(userId: String): Flowable<User> {
        return userDao.findUserById(userId)
            .subscribeOn(Schedulers.io())
    }

    fun updateUser(user: User): Completable {
        return Completable.create {
            Timber.d("Updating user ${user.name}.")
            userDao.update(user)
            it.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun addUser(user: User): Completable {
        return Completable.create {
            Timber.d("Adding user $user to database.")
            userDao.insert(user)
            it.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun deleteUser(user: User): Completable {
        return Completable.create {
            Timber.d("Deleting user ${user.name} from database.")
            userDao.delete(user)
            it.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun findDeviceUser(): Flowable<User> {
        val id = getDeviceId()
        return findUserById(id)
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun createDeviceUser(): Completable {
        Timber.d("Creating device user")
        val id = de.tub.affinity3.android.util.getDeviceId(context)
        val user = User(id, UsernameGenerator.generate())
        return addUser(user)
    }
}
