package de.tub.affinity3.android.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.MovieAllRatings
import de.tub.affinity3.android.activities.MovieDetailsActivity
import de.tub.affinity3.android.constants.ListType
import de.tub.affinity3.android.ui.movielist.MoviesAdapter
import de.tub.affinity3.android.view_models.ProfileViewModel
import de.tub.affinity3.android.util.gone
import de.tub.affinity3.android.util.hideKeyboard
import de.tub.affinity3.android.util.showSoftKeyboard
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_profile.buttonEdit
import kotlinx.android.synthetic.main.fragment_profile.recyclerRatedMovies
import kotlinx.android.synthetic.main.fragment_profile.recyclerWatchlist
import kotlinx.android.synthetic.main.fragment_profile.textRatedMoviesHeader
import kotlinx.android.synthetic.main.fragment_profile.textUserName
import kotlinx.android.synthetic.main.fragment_profile.textWatchlistHeader
import timber.log.Timber

class ProfileFragment : BaseRxFragment() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(ProfileViewModel::class.java)
    }

    private val watchlistAdapter = createMoviesAdapter()
    private val ratedMoviesAdapter = createMoviesAdapter()

    private fun createMoviesAdapter(): MoviesAdapter {
        return MoviesAdapter(ListType.LIST, emptyList(), object : MoviesAdapter.MovieItemClickListener {
            override fun onItemClicked(movie: MovieAllRatings) {
                val bundle = Bundle()
                bundle.putString(MovieDetailsActivity.ARG_MOVIE_ID, movie.imdbID)
                findNavController().navigate(R.id.action_profileFragment_to_movieDetailsActivity, bundle)
            }

            override fun onAddToWatchlistClicked(movie: MovieAllRatings, position: Int) {
                viewModel.onAddToWatchlistClicked(movie)
            }

            override fun onRatingClicked(movie: MovieAllRatings) {
                viewModel.onRatingClicked(movie, requireContext())
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setTitle(R.string.profile)
        setupRecycler()
        bindViewModel()
        setupViews()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
            }
            else ->
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupRecycler() {
        // Set the adapter
        with(recyclerWatchlist) {
            layoutManager = LinearLayoutManager(context)
            adapter = watchlistAdapter
            isNestedScrollingEnabled = false
        }

        with(recyclerRatedMovies) {
            layoutManager = LinearLayoutManager(context)
            adapter = ratedMoviesAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun bindViewModel() {
        viewModel.fetchWatchlist()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { textWatchlistHeader?.gone = it.isEmpty() }
                .subscribe(watchlistAdapter::replaceData)
                .addTo(disposeBag)

        viewModel.fetchRatedMovies()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { textRatedMoviesHeader?.gone = it.isEmpty() }
            .subscribeBy(
                onNext = {
                    ratedMoviesAdapter.replaceData(it)
                },
                onError = {
                    Timber.d(it)
                })
                .addTo(disposeBag)

        viewModel.getUsername()
                .subscribe {
                    textUserName.text.clear()
                    textUserName.append(it)
                }
                .addTo(disposeBag)
    }

    private fun setupViews() {
        buttonEdit.setOnClickListener {
            textUserName.isFocusable = true
            textUserName.isFocusableInTouchMode = true
            textUserName.requestFocus()
            activity?.showSoftKeyboard(textUserName)
        }

        textUserName.setOnEditorActionListener(TextView.OnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                if (event == null || !event.isShiftPressed) {
                    // the user is done typing.
                    viewModel.didUpdateUsername(textView.text.toString())
                    textUserName.isFocusable = false
                    textUserName.isFocusableInTouchMode = false
                    activity?.hideKeyboard()
                    return@OnEditorActionListener true
                }
            }
            false
        })
    }

    companion object {
        const val PREF_USERNAME_KEY = "pref_username"
    }
}
