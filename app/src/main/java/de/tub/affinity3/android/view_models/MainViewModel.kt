package de.tub.affinity3.android.view_models

import android.app.Application
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.repositories.RecommendationRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject

class MainViewModel(application: Application) : BaseRxViewModel(application) {

    private val recommendationRepository = RecommendationRepository(application)

    private val ratingsRepository = RatingRepository(application)

    val noRatingsSignal = PublishSubject.create<Boolean>()

    val badgeText = PublishSubject.create<String>()

    init {
        recommendationRepository.countNew()
            .observeOn(AndroidSchedulers.mainThread())
            .map { count ->
                if (count == 0) {
                    return@map ""
                } else {
                    return@map if (count > ELLIPSIZE_COUNT_THRESHOLD) "9+" else count.toString()
                }
            }
            .subscribe(badgeText::onNext)
            .addTo(disposeBag)

        ratingsRepository.getOwnRatings()
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.isEmpty() }
            .subscribe(noRatingsSignal::onNext)
            .addTo(disposeBag)
    }

    companion object {
        private const val ELLIPSIZE_COUNT_THRESHOLD = 9
    }
}
