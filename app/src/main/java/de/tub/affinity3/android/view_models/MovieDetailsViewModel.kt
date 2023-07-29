package de.tub.affinity3.android.ui.moviedetails

import android.app.Application
import android.content.Context
import androidx.annotation.ColorRes
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.Movie
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.view_models.BaseMovieViewModel
import de.tub.affinity3.android.util.getDeviceId
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class MovieDetailsViewModel(application: Application) : BaseMovieViewModel(application) {

    val movie = BehaviorSubject.create<Movie>()
    val ratings = BehaviorSubject.create<List<Rating>>()

    val ratingsViewModel = BehaviorSubject.create<RatingsViewModel>()

    var movieId: String? = null

    fun findMovie() {
        val id = movieId ?: return
        movieRepository.findById(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    movie.onNext(it)
                    findRatings(it)
                }.addTo(disposeBag)
    }

    private fun findRatings(movie: Movie) {
        ratingRepository.getByMovieId(movie.imdbID)
                .subscribe { ratings ->
                    this.ratings.onNext(ratings)
                    processRatings(movie, ratings)
                }.addTo(disposeBag)
    }

    private fun processRatings(movie: Movie, ratings: List<Rating>) {
        var userRating: Rating? = null
        val userId = getDeviceId(getApplication())
        val nearByRatings = mutableListOf<Rating>()
        for (rating in ratings) {
            if (rating.userId == userId) {
                userRating = rating
            } else {
                nearByRatings.add(rating)
            }
        }

        val nearbyRatingsCount = "(${nearByRatings.size} Ratings)"
        val nearbyAverageRating = if (nearByRatings.size > 0) String.format("%.1f", nearByRatings.map { it.score }.average()) else null

        var metascoreColor: Int = R.color.darkGrey

        movie.metascore?.toIntOrNull()?.let { metascore ->
            if (metascore <= 40) {
                metascoreColor = R.color.metacritic_red
            } else if (metascore <= 60) {
                metascoreColor = R.color.metacritic_orange
            } else {
                metascoreColor = R.color.metacritic_green
            }
        }

        this.ratingsViewModel.onNext(RatingsViewModel(
                userRating = userRating?.score?.toString(),
                nearbyRating = nearbyAverageRating,
                nearbyRatingsCount = nearbyRatingsCount,
                imdbRating = movie.imdbRating,
                imdbRatingsCount = "(${movie.imdbVotes})",
                metascoreRating = movie.metascore,
                metascoreColor = metascoreColor
        ))
    }

    fun onAddToWatchlistClicked() {
        val movie = movie.value ?: return
        addMovieToWatchlist(movieId = movie.imdbID)
                .subscribe {
                    Timber.d("Movie updated")
                    findMovie()
                }.addTo(disposeBag)
    }

    fun onRatingClicked(context: Context) {
        val movie = movie.value ?: return
        val ratings = ratings.value ?: emptyList()
        super.onRatingClicked(movie.imdbID, movie.title, ratings, context)
    }
}

data class RatingsViewModel(
    val userRating: String?,
    val nearbyRating: String?,
    val nearbyRatingsCount: String?,
    val imdbRating: String?,
    val imdbRatingsCount: String?,
    val metascoreRating: String?,
    @ColorRes val metascoreColor: Int
)
