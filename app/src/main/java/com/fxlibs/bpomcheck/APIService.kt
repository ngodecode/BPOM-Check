package com.fxlibs.bpomcheck

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface APIService {

    @GET("/")
    fun getSession(): Call<String>

    @GET("index.php/home/produk/{session_id}/all/row/10/page/1/order/4/DESC/search/0/{bpom_id}")
    fun getInfo(@Header("Cookie") cookieId:String, @Path(value = "session_id") sessionId:String, @Path(value = "bpom_id") bpomId:String): Call<String>

    @GET("index.php/home/detil/{session_id}/produk/{detail_id}")
    fun getDetail(@Header("Cookie") cookieId:String, @Path(value = "session_id") sessionId:String, @Path(value = "detail_id") detailId:String): Call<String>


    companion object {
        private var mClient : OkHttpClient? = null
        fun get() : APIService {
            if (mClient == null) {
                mClient = buildClient()
            }
            val retrofit = Retrofit.Builder().baseUrl("https://cekbpom.pom.go.id")
                .client(mClient)
                .addConverterFactory(ConverterFactory())
                .build()

            return retrofit.create(APIService ::class.java)
        }

        private fun buildClient() : OkHttpClient {
            val trustAllCerts: Array<TrustManager> = arrayOf (
                object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> {return arrayOf()}
                }
            )
            return OkHttpClient.Builder().apply {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                sslSocketFactory (
                    SSLContext.getInstance("SSL").apply{init(null, trustAllCerts, SecureRandom())}.socketFactory,
                    trustAllCerts[0] as X509TrustManager
                )
            }.build()
        }
    }

}