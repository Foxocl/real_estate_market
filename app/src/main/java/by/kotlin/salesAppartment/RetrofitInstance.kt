package by.kotlin.salesAppartment

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient

object RetrofitInstance {
    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "RealEstateApp/1.0 (amiray701@gmail.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    val nominatimApi: NominatimApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApiService::class.java)
    }
}