package de.tub.affinity3.android.view_models

import android.app.Application
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.Rating
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject

class InitialRatingViewModel(application: Application) : MoviesViewModel(application) {

    val ratingFinishedSignal = BehaviorSubject.create<Boolean>()

    val title = BehaviorSubject.create<String>()

    val ownRatings = BehaviorSubject.create<List<Rating>>()

    init {
        ratingRepository.getOwnRatings()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val requiredCount = MIN_REQUIRED_RATINGS_COUNT - it.size
                val finished = requiredCount <= 0
                ratingFinishedSignal.onNext(finished)

                title.onNext(if (finished) {
                    application.getString(R.string.finished)
                } else {
                    application.getString(R.string.rate_movies_you_liked, requiredCount)
                })

                ratingFinishedSignal.onNext(requiredCount <= 0)

                ownRatings.onNext(it)
            }.addTo(disposeBag)
    }

    companion object {
        const val MIN_REQUIRED_RATINGS_COUNT = 3
    }
}
