package fr.epf.mm.gestionclient

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import fr.epf.mm.gestionclient.model.Country
import okio.IOException
import retrofit2.HttpException
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptorFactory


private const val TAG = "ListCountriesActivity"

class ListCountriesActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var recyclerView: RecyclerView
    lateinit var retrofit: Retrofit
    lateinit var countryService: CountryService
    lateinit var countries: List<Country>
    lateinit var filteredCountries: List<Country>
    var minPopulationFilter: Int = 0
    var maxPopulationFilter: Int = -1
    var minSizeFilter: Int = 0
    var maxSizeFilter: Int = -1
    var isFiltered = false
    var isFavoriesOnly = false
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_countries)

        mapView = findViewById(R.id.search_mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initRetrofit()

        recyclerView =
            findViewById<RecyclerView>(R.id.list_countries_recyclerview)

        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val countryNameSearchView = findViewById<SearchView>(R.id.search_country_searchview)

        countryNameSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (isFavoriesOnly || !isNetworkAvailable()) {
                    retrieveLikedCountries()
                    filterCountriesByPopulationBySize()
                } else {
                    fetchCountryByName(query ?: "")
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (isFavoriesOnly || !isNetworkAvailable()) {
                    retrieveLikedCountries()
                    filterCountriesByPopulationBySize()
                } else {
                    fetchCountryByName(newText ?: "")
                }
                return false
            }
        })

        val switchAdvancedSearchView = findViewById<Switch>(R.id.advanced_search_switch)
        val advancedSearchLayout = findViewById<LinearLayout>(R.id.advanced_search_layout)
        advancedSearchLayout.visibility = View.GONE
        switchAdvancedSearchView.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                advancedSearchLayout.visibility = View.VISIBLE
                isFiltered = true
            } else {
                advancedSearchLayout.visibility = View.GONE
                isFiltered = false
                filterCountriesByPopulationBySize()
            }
        }

        val switchMapSearchView = findViewById<Switch>(R.id.map_search_switch)
        val mapSearchLayout = findViewById<MapView>(R.id.search_mapView)
        mapSearchLayout.visibility = View.GONE
        switchMapSearchView.setOnCheckedChangeListener { _, isChecked ->
            mapSearchLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        val switchFavoriesView = findViewById<Switch>(R.id.favories_only_switch)
        switchFavoriesView.setOnCheckedChangeListener { _, isChecked ->
            isFavoriesOnly = isChecked
            if (isFavoriesOnly) {
                retrieveLikedCountries()
            } else {
                fetchAllCountries()
            }
        }

        val minPopulationSeekBar = findViewById<SeekBar>(R.id.min_population_seekbar)
        val minPopilationtextView = findViewById<TextView>(R.id.min_population_res_textview)
        val maxPopulationSeekBar = findViewById<SeekBar>(R.id.max_population_seekbar)
        val maxPopilationtextView = findViewById<TextView>(R.id.max_population_res_textview)
        val minSizeSeekBar = findViewById<SeekBar>(R.id.min_size_seekbar)
        val minSizetextView = findViewById<TextView>(R.id.min_size_res_textview)
        val maxSizeSeekBar = findViewById<SeekBar>(R.id.max_size_seekbar)
        val maxSizetextView = findViewById<TextView>(R.id.max_size_res_textview)

        fun getAdjustedProgress(progress: Int, seekBarMax: Int): Int {
            return (Math.pow(progress.toDouble() / seekBarMax, 2.0) * seekBarMax).toInt()
        }

        minPopulationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                minPopulationFilter = getAdjustedProgress(progress, seekBar?.max!!) * 1000
                minPopilationtextView.text = String.format("%,d k", minPopulationFilter/1000)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                filterCountriesByPopulationBySize()
            }
        })

        maxPopulationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxPopulationFilter = getAdjustedProgress(progress, seekBar?.max!!) * 1000
                maxPopilationtextView.text = String.format("%,d k", maxPopulationFilter/1000)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                filterCountriesByPopulationBySize()
            }
        })

        minSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                minSizeFilter = getAdjustedProgress(progress, seekBar?.max!!)
                minSizetextView.text = String.format("%,d km²", minSizeFilter)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                filterCountriesByPopulationBySize()
            }
        })

        maxSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxSizeFilter = getAdjustedProgress(progress, seekBar?.max!!)
                maxSizetextView.text = String.format("%,d km²", maxSizeFilter)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                filterCountriesByPopulationBySize()
            }
        })

        if (isNetworkAvailable()) fetchAllCountries() else retrieveLikedCountries()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.list_countries, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_synchro -> {
                val countryNameSearchView = findViewById<SearchView>(R.id.search_country_searchview)
                if (countryNameSearchView.query.isEmpty()) {
                    fetchAllCountries()
                } else {
                    fetchCountryByName(countryNameSearchView.query.toString())
                }
            }
            R.id.action_play_country_guess -> {
                val intent = Intent(this, CountryGuessActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun filterCountriesByPopulationBySize() {
        if (isFiltered) {
            filteredCountries =
                countries.filter { it.population in minPopulationFilter..maxPopulationFilter }
            filteredCountries =
                filteredCountries.filter { it.size.toInt() in minSizeFilter..maxSizeFilter }
        } else {
            filteredCountries = countries.map { it.copy() }
        }

        updateSearchMap()
        val adapter = CountryAdapter(filteredCountries, this@ListCountriesActivity)
        recyclerView.adapter = adapter
    }

    private fun fetchAllCountries() {
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

                if (maxPopulationFilter == -1) maxPopulationFilter =
                    countries.maxOf { it.population }
                if (maxSizeFilter == -1) maxSizeFilter = countries.maxOf { it.size.toInt() }

                filterCountriesByPopulationBySize()

            } catch (e: HttpException) {
                when(e.code()) {
                    404 -> {
                        Toast.makeText(this@ListCountriesActivity, "Country not found", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Country not found", e)
                    }
                    429 -> {
                        Toast.makeText(this@ListCountriesActivity, "Too many requests. Please try again later.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Too many requests", e)
                    }
                    else -> {
                        Toast.makeText(this@ListCountriesActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "An error occurred", e)
                    }
                }
                val adapter = CountryAdapter(emptyList(), this@ListCountriesActivity)
                recyclerView.adapter = adapter
            } catch (e: IOException) {
                Toast.makeText(this@ListCountriesActivity, "Error during fetch", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error during fetch", e)
                val adapter = CountryAdapter(emptyList(), this@ListCountriesActivity)
                recyclerView.adapter = adapter
            }
        }
    }

    private fun fetchCountryByName(name: String) {
        if (name.isEmpty()) {
            fetchAllCountries()
            return
        }
        runBlocking {
            try {
                val countriesData = countryService.getCountryByName(name)

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

                filterCountriesByPopulationBySize()

            } catch (e: HttpException) {
                when(e.code()) {
                    404 -> {
                        Toast.makeText(this@ListCountriesActivity, "Country not found", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Country not found", e)
                    }
                    429 -> {
                        Toast.makeText(this@ListCountriesActivity, "Too many requests. Please try again later.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Too many requests", e)
                    }
                    else -> {
                        Toast.makeText(this@ListCountriesActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "An error occurred", e)
                    }
                }
                val adapter = CountryAdapter(emptyList(), this@ListCountriesActivity)
                recyclerView.adapter = adapter
            } catch (e: IOException) {
                Toast.makeText(this@ListCountriesActivity, "Error during fetch", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error during fetch", e)
                val adapter = CountryAdapter(emptyList(), this@ListCountriesActivity)
                recyclerView.adapter = adapter
            }
        }
    }

    private fun initRetrofit() {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl("https://www.apicountries.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()

        countryService =
            retrofit.create(CountryService::class.java)
    }

    private fun retrieveLikedCountries() {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("countries", Context.MODE_PRIVATE)
        val gson = Gson()
        val likedCountriesJson = sharedPreferences.getString("liked_countries", "[]")
        val likedCountries = gson.fromJson(likedCountriesJson, Array<Country>::class.java).asList()

        countries = likedCountries.map { it.copy() }

        filterCountriesByPopulationBySize()
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

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        updateSearchMap()
    }

    fun updateSearchMap() {
        googleMap?.clear()
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(0.0, 0.0)))

        for (country in filteredCountries) {
            Glide.with(this)
                .asBitmap()
                .load(country.flag)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(25)))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val aspectRatio = resource.width.toFloat() / resource.height.toFloat()
                        val height = 65
                        val width = Math.round(height * aspectRatio)

                        val resizedBitmap = Bitmap.createScaledBitmap(resource, width, height, false)

                        val markerOptions = MarkerOptions()
                            .position(country.latlng)
                            .title(country.name)
                            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)) // Set the resized flag as the marker icon
                        googleMap?.addMarker(markerOptions)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        val markerOptions =
                            MarkerOptions().position(country.latlng).title(country.name)
                        googleMap?.addMarker(markerOptions)
                    }
                })

        }

        googleMap?.setOnMarkerClickListener { marker ->
            val selectedCountry = filteredCountries.find { it.name == marker.title }

            selectedCountry?.let {
                val intent = Intent(this, DetailsCountryActivity::class.java).apply {
                    putExtra(DetailsCountryActivity.COUNTRY_ID_EXTRA, it)
                }
                startActivity(intent)
            }

            true
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
}