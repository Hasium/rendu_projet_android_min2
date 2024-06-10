package fr.epf.mm.gestionclient
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LatLongCountryService {
    @GET("countryCodeJSON")
    suspend fun getCountriesByPopulation(
        @Query("lat") lat: Double,
        @Query("lng") long: Double,
        @Query("username") username: String = "hasium"
    ): LatLongCountryData
}

data class LatLongCountryData(
    val countryCode: String,
    val countryName: String
)
