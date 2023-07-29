package de.tub.affinity3.android.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import de.tub.affinity3.android.R
import de.tub.affinity3.android.ui.moviedetails.MovieDetailsViewModel
import de.tub.affinity3.android.util.PicassoTrustAll
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_movie_details.collapsingToolbar
import kotlinx.android.synthetic.main.activity_movie_details.imagePoster
import kotlinx.android.synthetic.main.activity_movie_details.textActors
import kotlinx.android.synthetic.main.activity_movie_details.textDirector
import kotlinx.android.synthetic.main.activity_movie_details.textGenre
import kotlinx.android.synthetic.main.activity_movie_details.textPlot
import kotlinx.android.synthetic.main.activity_movie_details.textRuntime
import kotlinx.android.synthetic.main.activity_movie_details.textWriter
import kotlinx.android.synthetic.main.activity_movie_details.textYear
import kotlinx.android.synthetic.main.activity_movie_details.toolbar
import kotlinx.android.synthetic.main.layout_ratings.contentUserRating
import kotlinx.android.synthetic.main.layout_ratings.imageMetascore
import kotlinx.android.synthetic.main.layout_ratings.imageUserRating
import kotlinx.android.synthetic.main.layout_ratings.textImdbRatingCount
import kotlinx.android.synthetic.main.layout_ratings.textImdbRatingValue
import kotlinx.android.synthetic.main.layout_ratings.textMetascoreRatingValue
import kotlinx.android.synthetic.main.layout_ratings.textNearbyRatingCount
import kotlinx.android.synthetic.main.layout_ratings.textNearbyRatingValue
import kotlinx.android.synthetic.main.layout_ratings.textOwnRatingDescription
import kotlinx.android.synthetic.main.layout_ratings.textOwnRatingValue

class MovieDetailsActivity : BaseRxActivity() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(MovieDetailsViewModel::class.java)
    }

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        setupToolbar()

        intent?.extras?.getString(ARG_MOVIE_ID)?.let { movieId ->
            viewModel.movieId = movieId
            viewModel.findMovie()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_movie_details, menu)
        bindViewModel()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.action_watchlist -> {
                viewModel.onAddToWatchlistClicked()
            }
            else ->
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun bindViewModel() {
        viewModel.movie.subscribe { movie ->
            collapsingToolbar.title = movie.title
            textYear.text = movie.year
            textRuntime.text = movie.runtime ?: "-"
            textGenre.text = movie.genre
            textPlot.text = movie.plot
            textActors.text = movie.actors ?: "-"
            textDirector.text = movie.director ?: "-"
            textWriter.text = movie.writer ?: "-"

            if (movie.poster?.isNotEmpty() == true) {
                PicassoTrustAll.getInstance(this)
                    ?.load(movie.poster)
                    ?.into(imagePoster)
            }

            val watchlistItem = this.menu?.getItem(0)
            watchlistItem?.icon =
                if (movie.isOnWatchlist) getDrawable(R.drawable.ic_bookmark_enabled) else getDrawable(
                    R.drawable.ic_bookmark_disabled
                )
        }.addTo(disposeBag)

        viewModel.ratingsViewModel
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                textImdbRatingValue.text = it.imdbRating ?: "-"
                textImdbRatingCount.text = it.imdbRatingsCount ?: "-"
                textMetascoreRatingValue.text = it.metascoreRating ?: "-"
                textImdbRatingValue.text = it.imdbRating ?: "-"
                textOwnRatingValue.text = it.userRating ?: "-"

                if (it.userRating == null) {
                    imageUserRating.setImageResource(R.drawable.ic_star_border)
                    textOwnRatingDescription.text = getString(R.string.rate_this)
                } else {
                    imageUserRating.setImageResource(R.drawable.ic_star)
                    textOwnRatingDescription.text = getString(R.string.your_rating)
                }

                textNearbyRatingValue.text = it.nearbyRating ?: "-"
                textNearbyRatingCount.text = it.nearbyRatingsCount
                imageMetascore.setBackgroundResource(it.metascoreColor)
            }.addTo(disposeBag)

        contentUserRating.setOnClickListener {
            viewModel.onRatingClicked(this)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }

    companion object {
        const val ARG_MOVIE_ID = "movieId"
    }
}
