package de.tub.affinity3.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.miguelcatalan.materialsearchview.MaterialSearchView
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.MovieAllRatings
import de.tub.affinity3.android.activities.MovieDetailsActivity
import de.tub.affinity3.android.constants.ListType
import de.tub.affinity3.android.ui.movielist.MoviesAdapter
import de.tub.affinity3.android.view_models.MoviesViewModel
import de.tub.affinity3.android.util.fadeIn
import de.tub.affinity3.android.util.fadeOut
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_movies.dimLayout
import kotlinx.android.synthetic.main.fragment_movies.recyclerMovies

open class MoviesFragment : BaseRxFragment() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(MoviesViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private val moviesAdapter =
        MoviesAdapter(ListType.LIST, emptyList(), object : MoviesAdapter.MovieItemClickListener {
            override fun onItemClicked(movie: MovieAllRatings) {
                val bundle = Bundle()
                bundle.putString(MovieDetailsActivity.ARG_MOVIE_ID, movie.imdbID)
                findNavController().navigate(
                    R.id.action_navigation_movielist_to_movieDetailsActivity,
                    bundle
                )
            }

            override fun onAddToWatchlistClicked(movie: MovieAllRatings, position: Int) {
                viewModel.onAddToWatchlistClicked(movie)
            }

            override fun onRatingClicked(movie: MovieAllRatings) {
                viewModel.onRatingClicked(movie, requireContext())
            }
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_movies, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_movies_fragment, menu)

        val item = menu.findItem(R.id.action_search)
        val searchView = activity?.findViewById<MaterialSearchView>(R.id.materialSearchView)
        searchView?.setMenuItem(item)
        searchView?.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewShown() {
                dimLayout.fadeIn(200)
            }

            override fun onSearchViewClosed() {
                dimLayout.fadeOut(200)
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.title_movies)
        setupRecycler()
        bindViewModel()

        dimLayout.setOnClickListener {
            activity?.findViewById<MaterialSearchView>(R.id.materialSearchView)?.closeSearch()
        }
    }

    private fun setupRecycler() {
        // Set the adapter
        with(recyclerMovies) {
            layoutManager = LinearLayoutManager(context)
            adapter = moviesAdapter
        }
    }

    private fun bindViewModel() {
        viewModel.fetchAllMovies()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(moviesAdapter::replaceData)
            .addTo(disposeBag)
    }
}
