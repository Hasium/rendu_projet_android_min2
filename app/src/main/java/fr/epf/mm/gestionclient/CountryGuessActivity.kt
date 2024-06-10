package fr.epf.mm.gestionclient

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import fr.epf.mm.gestionclient.model.Country
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions

private const val TAG = "CountryGuessActivity"

class CountryGuessActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var countryToGuess: Country
    lateinit var retrofitApiCountries: Retrofit
    lateinit var retrofitlatLongCountry: Retrofit
    lateinit var countryService: CountryService
    lateinit var countries: List<Country>
    lateinit var latLongCountryService: LatLongCountryService
    lateinit var scoreTextView: TextView
    lateinit var nextGuessButton: Button
    var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_guess)

        mapView = findViewById(R.id.country_guess_mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initRetrofitApiCountries()
        initRetrofitApiLatLongCountry()
        fetchAllCountriesAndInitGame()

        val countryNameTextView = findViewById<TextView>(R.id.country_to_find_text_view)
        countryNameTextView.text = countryToGuess.name

        scoreTextView = findViewById(R.id.score_country_guess_textview)
        scoreTextView.text = "$score points"

        nextGuessButton = findViewById(R.id.next_guess_button)

        nextGuessButton.setOnClickListener {
            googleMap?.clear()
            countryToGuess = countries.random()
            countryNameTextView.text = countryToGuess.name
            nextGuessButton.isEnabled = false
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 1.0f))

        googleMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

        googleMap?.setOnMapClickListener { latLng ->
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng))

            if (isCloseEnough(latLng)) {
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                score += 10000
            } else {
                Toast.makeText(this, "Incorrect. Look at the map", Toast.LENGTH_SHORT).show()
                // Get the distance between the marker and the country to guess
                val distance = FloatArray(1)
                Location.distanceBetween(
                    latLng.latitude,
                    latLng.longitude,
                    countryToGuess.latlng.latitude,
                    countryToGuess.latlng.longitude,
                    distance
                )
                score += (10000 - distance[0]/1000).toInt()
            }

            Glide.with(this)
                .asBitmap()
                .load(countryToGuess.flag)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(25)))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val aspectRatio = resource.width.toFloat() / resource.height.toFloat()
                        val height = 65
                        val width = Math.round(height * aspectRatio)

                        val resizedBitmap =
                            Bitmap.createScaledBitmap(resource, width, height, false)

                        val markerOptions = MarkerOptions()
                            .position(countryToGuess.latlng)
                            .title(countryToGuess.name)
                            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)) // Set the resized flag as the marker icon
                        googleMap?.addMarker(markerOptions)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        val markerOptions =
                            MarkerOptions().position(countryToGuess.latlng)
                                .title(countryToGuess.name)
                        googleMap?.addMarker(markerOptions)
                    }
                })

            scoreTextView.text = "$score points"
            nextGuessButton.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun isCloseEnough(latLng: LatLng): Boolean {
        var isTheSameCountry = false
        runBlocking {
            try {
                val latLongCountryData = latLongCountryService.getCountriesByPopulation(
                    latLng.latitude,
                    latLng.longitude
                )

                isTheSameCountry =
                    latLongCountryData.countryCode == countryToGuess.alpha2Code || latLongCountryData.countryName == countryToGuess.name

            } catch (e: IOException) {
                Toast.makeText(this@CountryGuessActivity, "Error during fetch", Toast.LENGTH_SHORT)
                    .show()
                Log.e(TAG, "Error during fetch", e)
            }
        }
        return isTheSameCountry
    }

    private fun fetchAllCountriesAndInitGame() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show()
            return
        }
        runBlocking {
            try {
                val countriesData = countryService.getAllCountries()

                countries = countriesData.map {
                    Country(
                        name = it.name,
                        capital = it.capital ?: "",
                        continent = it.region,
                        size = it.area,
                        population = it.population,
                        flag = it.flags?.png ?: "",
                        latlng = if (it.latlng != null) LatLng(
                            it.latlng[0],
                            it.latlng[1]
                        ) else LatLng(0.0, 0.0),
                        alpha2Code = it.alpha2Code
                    )
                }

                countryToGuess = countries.random()

            } catch (e: IOException) {
                Toast.makeText(this@CountryGuessActivity, "Error during fetch", Toast.LENGTH_SHORT)
                    .show()
                Log.e(TAG, "Error during fetch", e)
            }
        }
    }

    private fun initRetrofitApiCountries() {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()

        retrofitApiCountries = Retrofit.Builder()
            .baseUrl("https://www.apicountries.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()

        countryService =
            retrofitApiCountries.create(CountryService::class.java)
    }

    private fun initRetrofitApiLatLongCountry() {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()

        retrofitlatLongCountry = Retrofit.Builder()
            .baseUrl("http://api.geonames.org/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()

        latLongCountryService =
            retrofitlatLongCountry.create(LatLongCountryService::class.java)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}