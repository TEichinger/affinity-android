package de.tub.affinity3.android.activities

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.miguelcatalan.materialsearchview.MaterialSearchView
import de.tub.affinity3.android.view_models.MainViewModel
import de.tub.affinity3.android.R
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseRxActivity() {

    private var showingInitialRatings = false

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        /**
         * Setting up bottom navigation
         */
        bottomNav.apply {
            setupWithNavController(
                findNavController(R.id.nav_host_fragment)
            )
        }


        /**
         * Setting up material search view
         */
        materialSearchView.apply {
            setVoiceSearch(true)
            showVoice(true)

            setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return if (query.isEmpty()) {
                        false
                    } else {
                        startSearchActivityWithQuery(query)
                        materialSearchView.closeSearch()
                        true
                    }
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
        }

        /**
         * Opens the [InitialRatingActivity] if the user
         * has not made any initial film ranking yet.
         */
        viewModel.noRatingsSignal
            .distinctUntilChanged()
            .subscribe { noRatings ->
                if (noRatings && !showingInitialRatings) {
                    startInitialRatingActivity()
                }
            }
            .addTo(disposeBag)
    }


    //====================================================================================
    // Private methods
    //====================================================================================

    /**
     * Starts (opens) the [SearchActivity] with the defined search query.
     *
     * @property query the given search query for the [SearchActivity]
     * @return [Unit]
     */
    private fun startSearchActivityWithQuery(query: String) {
        startActivity(
            Intent(this, SearchActivity::class.java).apply {
                action = Intent.ACTION_SEARCH
                putExtra(SearchManager.QUERY, query)
            }
        )
    }

    /**
     * Starts (opens) the [InitialRatingActivity].
     *
     * @return [Unit]
     */
    private fun startInitialRatingActivity() {
        startActivity(
            Intent(this, InitialRatingActivity::class.java)
        )
        showingInitialRatings = true
    }


    //====================================================================================
    // Overrides
    //====================================================================================

    override fun onResume() {
        super.onResume()
        showingInitialRatings = false

        val myPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val showExperimentsMenu = myPrefs.getBoolean(getString(R.string.pref_experiments_menu_key), false)

        bottomNav.menu.findItem(R.id.navigation_experiment).setVisible(showExperimentsMenu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }

        /**
         * If we got here, the user's action was not recognized.
         * Invoke the superclass to handle it.
         */
        return super.onOptionsItemSelected(item)
    }

    override fun setTitle(titleId: Int) {
        supportActionBar?.setTitle(titleId)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }

    override fun onBackPressed() {
        if (materialSearchView.isSearchOpen) {
            materialSearchView.closeSearch()

            return
        }

        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data)

            return
        }

        when (requestCode) {
            //=====================================================================================
            MaterialSearchView.REQUEST_VOICE -> {
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                if (matches.isNullOrEmpty()) {
                    return
                }

                val searchWord = matches[0]

                if (!TextUtils.isEmpty(searchWord)) {
                    materialSearchView.setQuery(searchWord, false)
                }

                return
            }
            //=====================================================================================
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
