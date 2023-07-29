package de.tub.affinity3.android.repositories

import androidx.test.InstrumentationRegistry
import de.tub.affinity3.android.persistence.AffinityDatabase
import java.io.IOException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MovieResponseRepositoryTest {

    private lateinit var db: AffinityDatabase

    private val context = InstrumentationRegistry.getTargetContext()

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getTargetContext()
        AffinityDatabase.TEST_MODE = true
        db = AffinityDatabase.getInstance(context)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // this is the test card in the Recommeded Menu that shows a dummy recommendation for the Dark Knight
    @Test
    @Throws(Exception::class)
    fun testFetchOfNonExistingMovie() {
        val movieRepository = MovieRepository(context)
        val movieId = "tt0096895"

        // test if API Request is made to fetch movie that doesn't exist in db
        val unknownMovie = movieRepository.findById(movieId)
            .firstOrError()
            .blockingGet()

        Assert.assertEquals("Batman", unknownMovie.title)

        // test if persisted response is returned
        val existingMovie = movieRepository.findById(movieId)
            .firstOrError()
            .blockingGet()

        Assert.assertEquals("Batman", existingMovie.title)

        movieRepository.deleteAll().blockingGet()
    }

    @Test
    fun testMovieSearch() {
        val movieRepository = MovieRepository(context)
        val search = movieRepository.search("Batman")
        val movies = search.blockingFirst()
        Assert.assertEquals(10, movies.count())
    }
}
