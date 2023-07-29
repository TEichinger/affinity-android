package de.tub.affinity3.android.activities

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import com.miguelcatalan.materialsearchview.MaterialSearchView
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.Search
import de.tub.affinity3.android.view_models.MoviesViewModel
import de.tub.affinity3.android.adapters.MoviesSearchAdapter
import de.tub.affinity3.android.util.fadeIn
import de.tub.affinity3.android.util.fadeOut
import de.tub.affinity3.android.util.gone
import de.tub.affinity3.android.util.visible
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_search.dimLayout
import kotlinx.android.synthetic.main.activity_search.emptyLayout
import kotlinx.android.synthetic.main.activity_search.materialSearchView
import kotlinx.android.synthetic.main.activity_search.recyclerMovies
import kotlinx.android.synthetic.main.activity_search.toolbar
import timber.log.Timber

class SearchActivity : BaseRxActivity() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(MoviesViewModel::class.java)
    }

    private val moviesAdapter =
        MoviesSearchAdapter(
            emptyList(),
            object : MoviesSearchAdapter.MovieItemClickListener {

                override fun onItemClicked(movie: Search) {
                    val bundle = Bundle()
                    bundle.putString(MovieDetailsActivity.ARG_MOVIE_ID, movie.imdbID)

                    val intent = Intent(this@SearchActivity, MovieDetailsActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                }
            })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        handleIntent(intent)

        setupRecycler()
        setupSearchView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == Activity.RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (matches != null && matches.size > 0) {
                val searchWrd = matches[0]
                if (!TextUtils.isEmpty(searchWrd)) {
                    materialSearchView.setQuery(searchWrd, false)
                }
            }

            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        val item = menu?.findItem(R.id.action_search)
        materialSearchView.setMenuItem(item)

        return true
    }

    override fun setTitle(titleId: Int) {
        supportActionBar?.setTitle(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        supportActionBar?.title = title
    }

    override fun onNewIntent(intent: Intent) {
        handleIntent(intent)
        super.onNewIntent(intent)
    }

    private fun setupSearchView() {
        materialSearchView.setVoiceSearch(true)
        materialSearchView.showVoice(true)
        materialSearchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return if (query.isEmpty()) {
                    false
                } else {
                    handleQuery(query)
                    materialSearchView.closeSearch()
                    true
                }
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        materialSearchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewShown() {
                dimLayout.fadeIn(200)
            }

            override fun onSearchViewClosed() {
                dimLayout.fadeOut(200)
            }
        })

        dimLayout.setOnClickListener {
            materialSearchView.closeSearch()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: ""
            handleQuery(query)
        }
    }

    private fun handleQuery(query: String) {
        title = query

        val progressBar = findViewById<ProgressBar>(R.id.progressBar1)
        progressBar.visible = true
        emptyLayout.gone = true

        viewModel.searchMovies(query)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { movies ->
                Timber.i("Found movies $movies")
                progressBar.visibility = View.GONE
                moviesAdapter.replaceData(movies)
                emptyLayout.gone = !movies.isEmpty()
            }
            .addTo(disposeBag)
    }

    private fun setupRecycler() {
        // Set the adapter
        with(recyclerMovies) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = moviesAdapter
        }
    }
}
