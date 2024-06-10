package fr.epf.mm.gestionclient.model

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize

enum class Gender {
    MAN, WOMAN
}

@Parcelize
data class Country(
    val name: String,
    val capital: String,
    val continent: String,
    val size: Float,
    val population: Int,
    val flag: String,
    val latlng: LatLng,
    val alpha2Code: String
) : Parcelable{
    companion object {
        fun generate(size : Int = 20) =
            (1..size).map {
                Country("Pays${it}",
                    "Capitale${it}",
                    "Continent${it}",
                    it*1000000f,
                    it*1000000,
                    "Drapeau${it}",
                    LatLng(0.0, 0.0),
                    "Code${it}"
                )
            }
    }
}