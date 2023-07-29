package de.tub.affinity3.android.view_models

import android.app.Application
import de.tub.affinity3.android.classes.data.MovieAllRatings
import de.tub.affinity3.android.classes.data.User
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class ProfileViewModel(application: Application) : MoviesViewModel(application) {

    private var user: User? = null

    private val username = BehaviorSubject.create<String>()

    fun getUsername(): Observable<String> = username.hide()

    init {
        fetchUser()
    }

    private fun fetchUser() {
        userRepository.findDeviceUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    user = it
                    username.onNext(it.name)
                }.addTo(disposeBag)
    }

    fun fetchWatchlist(): Flowable<List<MovieAllRatings>> {
        return movieRepository.findAllOnWatchlist()
    }

    fun fetchRatedMovies(): Flowable<List<MovieAllRatings>> {
        return ratingRepository.getOwnRatings()
                .flatMap {
                    val ids = it.map { it.movieId }
                    movieRepository.findAllWithIds(ids)
                }
    }

    fun didUpdateUsername(userName: String) {
        user?.let {
            it.name = userName
            userRepository.updateUser(it).subscribe {
                Timber.d("User updated")
            }.addTo(disposeBag)
        }
    }
}
