package fr.epf.mm.gestionclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import fr.epf.mm.gestionclient.model.Country
import java.text.NumberFormat
import java.util.Locale

class DetailsCountryActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val COUNTRY_ID_EXTRA = "COUNTRY_ID_EXTRA"
    }

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_country)

        mapView = findViewById(R.id.details_country_mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val countryNameTextView =
            findViewById<TextView>(R.id.details_country_name_textview)
        val capitalTextView = findViewById<TextView>(R.id.capital_name_textView)
        val populationTextView = findViewById<TextView>(R.id.population_text_textView)
        val sizeTextView = findViewById<TextView>(R.id.size_text_textview)
        val continentTextView = findViewById<TextView>(R.id.continent_name_textView)

        val imageView = findViewById<ImageView>(R.id.details_country_imageview)

        intent.extras?.apply {
            val country = getParcelable(COUNTRY_ID_EXTRA) as? Country

            country?.let {

                val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
                val formattedPopulation = formatter.format(it.population)
                val formattedSize = formatter.format(it.size)

                countryNameTextView.text = it.name
                capitalTextView.text = it.capital
                populationTextView.text = formattedPopulation
                sizeTextView.text = formattedSize.plus(" km²")
                continentTextView.text = it.continent
                Glide.with(this@DetailsCountryActivity)
                    .load(country.flag)
                    .into(imageView)
            }
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

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        intent.extras?.apply {
            val country = getParcelable(COUNTRY_ID_EXTRA) as? Country

            country?.let {
                val markerOptions = MarkerOptions().position(it.latlng).title(it.name)
                googleMap?.addMarker(markerOptions)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it.latlng, 3f))
            }
        }
    }
}