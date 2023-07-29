package de.tub.affinity3.android.views

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.mindorks.placeholderview.SwipeDirection
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.swipe.SwipeIn
import com.mindorks.placeholderview.annotations.swipe.SwipeInDirectional
import com.mindorks.placeholderview.annotations.swipe.SwipeOut
import com.mindorks.placeholderview.annotations.swipe.SwipeOutDirectional
import de.tub.affinity3.android.R
import de.tub.affinity3.android.util.PicassoTrustAll
import de.tub.affinity3.android.view_models.RecommendationsViewModel
import timber.log.Timber

@Layout(R.layout.item_recommendation)
class RecommendationCard(
        private val context: Context,
        private val movieRecommendation: RecommendationsViewModel.MovieRecommendation,
        private val listener: SwipeListener
) {

    interface SwipeListener {
        fun onDirectionSwiped(
                movieRecommendation: RecommendationsViewModel.MovieRecommendation,
                direction: SwipeDirection
        )
    }

    @View(R.id.imagePoster)
    lateinit var imagePoster: ImageView

    @View(R.id.textTitle)
    lateinit var textTitle: TextView

    @View(R.id.textYearAndDirector)
    lateinit var textYearAndDirector: TextView

    @View(R.id.textDuration)
    lateinit var textDuration: TextView

    @View(R.id.textGenre)
    lateinit var textGenre: TextView

    @View(R.id.textPlot)
    lateinit var textPlot: TextView

    @View(R.id.textRating)
    lateinit var textRating: TextView

    @SuppressLint("SetTextI18n")
    @Resolve
    fun onResolved() {
        movieRecommendation.movie.poster?.let {
            PicassoTrustAll.getInstance(context)
                ?.load(it)
                ?.placeholder(R.drawable.ic_movie_placeholder)
                ?.into(imagePoster)
        }
        textTitle.text = movieRecommendation.movie.title
        textDuration.text = movieRecommendation.movie.runtime
        textYearAndDirector.text =
            "${movieRecommendation.movie.year} - ${movieRecommendation.movie.director}"
        textGenre.text = movieRecommendation.movie.genre
        textRating.text = movieRecommendation.movie.imdbRating
        textPlot.text = movieRecommendation.movie.plot
    }

    @Click(R.id.imagePoster)
    fun onClick() {
        Timber.d("imagePoster click")
    }

    @SwipeOutDirectional
    fun onSwipeOutDirectional(direction: SwipeDirection) {
        listener.onDirectionSwiped(movieRecommendation, direction)
    }

    @SwipeInDirectional
    fun onSwipeInDirectional(direction: SwipeDirection) {
        listener.onDirectionSwiped(movieRecommendation, direction)
    }

    @SwipeIn
    fun onSwipeIn() {
        listener.onDirectionSwiped(movieRecommendation, SwipeDirection.RIGHT)
    }

    @SwipeOut
    fun onSwipeOut() {
        listener.onDirectionSwiped(movieRecommendation, SwipeDirection.LEFT)
    }
}
