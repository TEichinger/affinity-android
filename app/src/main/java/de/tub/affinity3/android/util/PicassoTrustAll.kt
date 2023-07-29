package de.tub.affinity3.android.util

import android.content.Context
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.Builder
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient

class PicassoTrustAll private constructor(context: Context) {
    companion object {
        private var mInstance: Picasso? = null
        fun getInstance(context: Context): Picasso? {
            if (mInstance == null) {
                PicassoTrustAll(context)
            }
            return mInstance
        }
    }

    init {
        val clientBuilder = OkHttpClient.Builder()
        clientBuilder.hostnameVerifier(HostnameVerifier { _, _ -> true })
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(
                x509Certificates: Array<X509Certificate>,
                s: String
            ) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(
                x509Certificates: Array<X509Certificate>,
                s: String
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        })
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            clientBuilder.sslSocketFactory(sc.socketFactory, trustAllCerts[0] as X509TrustManager)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val client = clientBuilder.build()
        mInstance = Builder(context)
            .downloader(OkHttp3Downloader(client))
            .listener { _, _, _ -> }.build()
    }
}
