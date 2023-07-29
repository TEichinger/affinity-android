package de.tub.affinity3.android

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import de.tub.affinity3.android.classes.data.Movie
import de.tub.affinity3.android.persistence.AffinityDatabase
import java.io.IOException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceTest {

    private lateinit var db: AffinityDatabase

    @Before
    fun createDb() {
        AffinityDatabase.TEST_MODE = true

        val context = InstrumentationRegistry.getTargetContext()
        db = AffinityDatabase.getInstance(context)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun addUsersAndList() {
        val moviesToCreate = arrayOf(
            Movie(
                imdbID = "12",
                title = "Batman",
                genre = "Horror",
                plot = "Scary",
                year = "2015"
            ),
            Movie(
                imdbID = "23",
                title = "Batman",
                genre = "Horror",
                plot = "Scary",
                year = "2010",
                isOnWatchlist = true
            )
        )

        // insert all movies
        db.movieDao().insert(*moviesToCreate)

        // list movies and compare list size
        val movies = db.movieDao().findAll()
        assert(movies.size == moviesToCreate.size)
    }

    // TODO: test here important queries
}
