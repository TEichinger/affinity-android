package de.tub.affinity3.android.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import com.opencsv.CSVWriter
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.util.getDeviceId
import de.tub.affinity3.android.util.toast
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import timber.log.Timber

class ExportService(val context: Context) {

    private val ratingsRepository = RatingRepository(context)

    @SuppressLint("SimpleDateFormat")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    @SuppressLint("SimpleDateFormat")
    private val fileDateFormatter = SimpleDateFormat("yyyy-MM-dd")

    init {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    fun exportRatings() {
        val ratings = ratingsRepository.getAllRatings().blockingFirst()
        processRatings(ratings)
    }

    private fun ratingToStringArray(rating: Rating): Array<String> {
        val data = arrayOf(rating.userId, rating.movieId, rating.score.toString(), dateFormatter.format(rating.date))
        return data
    }

    private fun processRatings(ratings: List<Rating>) {
        val deviceId = getDeviceId(context)
        val filename = "ratings_${deviceId}_${fileDateFormatter.format(Date())}.csv"
        val file = File(context.externalCacheDir, filename)
        val data = ratings.map { ratingToStringArray(it) }

        writeCSVFile(data, file)
        val uri = Uri.fromFile(file)

        if (file.exists()) {
            sendEmail(uri)
        } else {
            context.toast("Could not find file!")
        }
    }

    private fun writeCSVFile(data: List<Array<String>>, file: File) {
        val writer: CSVWriter

        writer = if (file.exists() && !file.isDirectory) {
            val mFileWriter = FileWriter(file, true)
            CSVWriter(mFileWriter)
        } else {
            CSVWriter(FileWriter(file))
        }
        writer.writeAll(data)
        writer.close()
    }

    private fun sendEmail(uri: Uri) {
        val emailIntent = Intent(Intent.ACTION_SEND)

        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("affinity.snet@gmail.com"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Affinity Ratings")
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
        emailIntent.type = "text/html"

        try {
            context.startActivity(emailIntent)
        } catch (e: Exception) {
            Timber.d(e)
            context.toast("No mail application found!")
        }
    }
}
