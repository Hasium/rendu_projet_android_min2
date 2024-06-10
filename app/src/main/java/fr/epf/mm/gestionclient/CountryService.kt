package fr.epf.mm.gestionclient

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CountryService {
    @GET("countries/name/{name}")
    suspend fun getCountryByName(@Path("name") name: String): List<CountryData>

    @GET("countries")
    suspend fun getCountriesByPopulation(
        @Query("population_from") minPopulation: Int,
        @Query("population_to") maxPopulation: Int
    ): List<CountryData>

    @GET("countries")
    suspend fun getAllCountries(): List<CountryData>
}

data class GetCountryByNameResult(val data: CountryData)
data class GetAllCountriesResult(val data: List<CountryData>)

data class CountryData(
    val name: String,
    val capital: String?,
    val region: String,
    val area: Float,
    val population: Int,
    val flags: FlagsData?,
    val latlng: List<Double>?,
    val alpha2Code: String
)

data class FlagsData(
    val png: String
)
