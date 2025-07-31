import com.kormax.universalreader.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory // Or KotlinxSerializationConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://54-88-67-169.sslip.io:3005/" // Your base URL

    val instance: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
        }
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging) // Add logging interceptor
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()) // Or KotlinxSerializationConverterFactory
            .build()
        retrofit.create(ApiService::class.java)
    }
}