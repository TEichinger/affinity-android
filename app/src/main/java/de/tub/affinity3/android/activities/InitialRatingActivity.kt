package de.tub.affinity3.android.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.MovieAllRatings
import de.tub.affinity3.android.constants.ListType
import de.tub.affinity3.android.ui.movielist.MoviesAdapter
import de.tub.affinity3.android.view_models.InitialRatingViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_initial_rating.recyclerMovies
import kotlinx.android.synthetic.main.activity_initial_rating.toolbar

class InitialRatingActivity : BaseRxActivity() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(InitialRatingViewModel::class.java)
    }

    private val moviesAdapter =
        MoviesAdapter(ListType.GRID, emptyList(), object : MoviesAdapter.MovieItemClickListener {
            override fun onItemClicked(movie: MovieAllRatings) {
                viewModel.onRatingClicked(movie, this@InitialRatingActivity)
            }

            override fun onAddToWatchlistClicked(movie: MovieAllRatings, position: Int) {
                // nothing to do
            }

            override fun onRatingClicked(movie: MovieAllRatings) {
                // nothing to do here since we update ratings on item click
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial_rating)
        setSupportActionBar(toolbar)
        setupRecycler()
        bindViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_initial_rating, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_finish)?.isVisible =
            viewModel.ratingFinishedSignal.value ?: false
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.action_finish -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecycler() {
        with(recyclerMovies) {
            adapter = moviesAdapter
        }
    }

    private fun bindViewModel() {
        viewModel.fetchAllMovies()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(moviesAdapter::replaceData)
            .addTo(disposeBag)

        viewModel.ratingFinishedSignal
            .subscribe {
                invalidateOptionsMenu()
            }.addTo(disposeBag)

        viewModel.title.subscribe(this::setTitle).addTo(disposeBag)
    }
}
