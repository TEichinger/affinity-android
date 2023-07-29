package de.tub.affinity3.android.view_models

import android.app.Application
import com.mindorks.placeholderview.SwipeDirection
import de.tub.affinity3.android.classes.data.Movie
import de.tub.affinity3.android.classes.data.Recommendation
import de.tub.affinity3.android.classes.data.Recommendation.Companion.FEEDBACK_DISLIKE
import de.tub.affinity3.android.classes.data.Recommendation.Companion.FEEDBACK_KNOWN
import de.tub.affinity3.android.classes.data.Recommendation.Companion.FEEDBACK_LIKE
import de.tub.affinity3.android.repositories.MovieRepository
import de.tub.affinity3.android.repositories.RecommendationRepository
import de.tub.affinity3.android.views.RecommendationCard
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class RecommendationsViewModel(application: Application) : BaseRxViewModel(application),
        RecommendationCard.SwipeListener {

    private var recommendationRepository: RecommendationRepository = RecommendationRepository(application)

    private var movieRepository: MovieRepository = MovieRepository(application)

    val toastSignal = PublishSubject.create<String>()

    val cardsCount = BehaviorSubject.createDefault(0)

    override fun onDirectionSwiped(movieRecommendation: MovieRecommendation, direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.LEFT,
            SwipeDirection.LEFT_BOTTOM,
            SwipeDirection.LEFT_TOP -> {

                movieRecommendation.movie.isOnWatchlist = false
                movieRepository.updateMovie(movieRecommendation.movie).subscribe()

                movieRecommendation.recommendation.feedback = FEEDBACK_DISLIKE
                recommendationRepository.updateRecommendation(movieRecommendation.recommendation).subscribe()
            }

            SwipeDirection.BOTTOM,
            SwipeDirection.TOP -> {
                Timber.d("Already watched this movie")
                // TODO: maybe tell user to rate this recommendation

                movieRecommendation.movie.isOnWatchlist = false
                movieRepository.updateMovie(movieRecommendation.movie).subscribe()

                movieRecommendation.recommendation.feedback = FEEDBACK_KNOWN
                recommendationRepository.updateRecommendation(movieRecommendation.recommendation).subscribe()
            }

            SwipeDirection.RIGHT,
            SwipeDirection.RIGHT_BOTTOM,
            SwipeDirection.RIGHT_TOP -> {
                movieRecommendation.movie.isOnWatchlist = true
                movieRepository.updateMovie(movieRecommendation.movie).subscribe()

                movieRecommendation.recommendation.feedback = FEEDBACK_LIKE
                recommendationRepository.updateRecommendation(movieRecommendation.recommendation).subscribe()
                toastSignal.onNext("Added ${movieRecommendation.movie.title} to watchlist")
            }
        }
    }

    fun fetchAllMovieRecommendations(): Flowable<MutableList<MovieRecommendation>> {
        Timber.d("Fetch new recommendations")
        
        //.take(1)// OMDBI movieIds are strings of length 9: e.g. "tt1457767"
        //.last()first(recommendation => recommendation.movieId.length == 9)
        return recommendationRepository.findAllNew()      // type: Flowable<MutableList<MovieRecommendation>>
                .observeOn(AndroidSchedulers.mainThread())
                .take(1) // list of < 1x MutableList<MovieRecommendation>>
                .flatMapIterable { it } // MutableList<MovieRecommendation>>
                .filter { it.movieId.length == 9 }
                .flatMap { recommendation ->        // takes a sequence and returns a sequence 
                    movieRepository.findById(recommendation.movieId).map {
                        MovieRecommendation(recommendation = recommendation, movie = it)
                    }.toList().toFlowable()
                }
    }

    fun newRecommendationsCount(): Observable<Int> {
        return recommendationRepository.countNew()
    }

    fun cardRemoved(remainingCount: Int) {
        cardsCount.onNext(remainingCount)
    }

    fun undoClicked() {
        cardsCount.onNext(cardsCount.value!! + 1)
    }

    data class MovieRecommendation(val recommendation: Recommendation, val movie: Movie)
}
