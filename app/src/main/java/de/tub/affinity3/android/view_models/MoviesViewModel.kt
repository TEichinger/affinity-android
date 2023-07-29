package de.tub.affinity3.android.view_models

import android.app.Application
import android.content.Context
import de.tub.affinity3.android.classes.Search
import de.tub.affinity3.android.classes.data.MovieAllRatings
import io.reactivex.Flowable
import io.reactivex.rxkotlin.addTo
import timber.log.Timber

open class MoviesViewModel(application: Application) : BaseMovieViewModel(application) {

    fun fetchAllMovies(): Flowable<List<MovieAllRatings>> = movieRepository.findAllWithRatings()

    fun searchMovies(query: String): Flowable<List<Search>> = movieRepository.search(query)

    fun onAddToWatchlistClicked(movie: MovieAllRatings) {
        movie.isOnWatchlist = !movie.isOnWatchlist
        addMovieToWatchlist(movie.imdbID)
            .subscribe {
                Timber.d("Movie updated")
            }.addTo(disposeBag)
    }

    fun onRatingClicked(movie: MovieAllRatings, context: Context) {
        onRatingClicked(movieId = movie.imdbID, movieTitle = movie.title, ratings = movie.ratings, context = context)
    }
}
