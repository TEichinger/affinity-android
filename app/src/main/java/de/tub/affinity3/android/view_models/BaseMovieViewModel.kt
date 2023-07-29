package de.tub.affinity3.android.view_models

import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import android.widget.RatingBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.repositories.MovieRepository
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.repositories.UserRepository
import io.reactivex.Completable
import io.reactivex.rxkotlin.addTo
import java.util.Calendar
import timber.log.Timber

abstract class BaseMovieViewModel(application: Application) : BaseRxViewModel(application) {

    protected val movieRepository: MovieRepository = MovieRepository(application)
    protected val userRepository: UserRepository = UserRepository(application)
    protected val ratingRepository: RatingRepository = RatingRepository(application)

    protected fun showAddOrUpdateRatingDialog(
        context: Context,
        movieTitle: String,
        rating: Rating?,
        handler: (Float) -> (Unit),
        deleteHandler: (() -> (Unit))? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context)
        val inflater = LayoutInflater.from(context)
        val dialogLayout = inflater.inflate(R.layout.rating_dialog, null)
        val ratingBar = dialogLayout.findViewById(R.id.ratingBar) as RatingBar

        rating?.score?.let {
            ratingBar.rating = it
            builder.setNeutralButton(R.string.delete) { _, _ ->
                deleteHandler?.invoke()
            }
        }

        builder.setTitle(R.string.rating)
        builder.setMessage("How would you rate $movieTitle?")
        builder.setView(dialogLayout)
        builder.setPositiveButton(R.string.rate) { _, _ ->
            handler(ratingBar.rating)
        }

        builder.setNegativeButton(R.string.cancel) { _, _ ->
        }

        val dialog = builder.create()
        dialog.show()
    }

    fun onRatingClicked(movieId: String, movieTitle: String, ratings: List<Rating>, context: Context) {
        val userId = userRepository.getDeviceId()

        val ownRating = ratings.firstOrNull { it.userId == userId }

        ownRating?.let {
            showAddOrUpdateRatingDialog(context, movieTitle, it, handler = { score ->
                it.score = score
                ratingRepository.updateRating(it).subscribe {
                    Timber.d("Rating was updated for %s", movieTitle)
                }.addTo(disposeBag)
            }, deleteHandler = {
                ratingRepository.deleteRatings(it).subscribe()
            })
        } ?: kotlin.run {
            showAddOrUpdateRatingDialog(context, movieTitle, null, handler = { score ->
                val date = Calendar.getInstance().time
                val newRating = Rating(
                        id = userId + movieId,
                        movieId = movieId,
                        userId = userId,
                        score = score,
                        date = date
                )
                ratingRepository.addRating(newRating).subscribe {
                    Timber.d("Rating created for %s", movieTitle)
                }.addTo(disposeBag)
            })
        }
    }

    protected fun addMovieToWatchlist(movieId: String): Completable {
        return movieRepository.findById(movieId)
                .firstOrError() // Only take one element here to avoid infinite loop (callback -> update -> callback -> ...)
                .flatMapCompletable {
                    it.isOnWatchlist = !it.isOnWatchlist
                    movieRepository.updateMovie(it)
                }
    }
}
