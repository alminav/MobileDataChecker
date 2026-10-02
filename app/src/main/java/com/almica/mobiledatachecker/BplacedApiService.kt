package com.almica.mobiledatachecker

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class ApiResponse(
    val status: String,
    val message: String,
)

interface BplacedApiService {
    @FormUrlEncoded
    @POST("location.php")
    suspend fun saveLocation(
        @Field("title") title: String,
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Field("altitude") altitude: Double,
        @Field("temperature") temperature: Float?,
    ): Response<ApiResponse>

    // Neue Methode zum Abrufen der Standorte
    @GET("get_locations.php")
    suspend fun fetchLocations(): Response<FetchLocationsResponse>
    // Neue Methode zum Löschen aller Einträge
    @POST("location_cleanup.php")
    suspend fun deleteAllLocations(): Response<ApiResponse>
    @FormUrlEncoded
    @POST("location_delete_filter.php")
    suspend fun deleteLocationByFilter(
        @Field("title") title: String
    ): Response<ApiResponse>
    @FormUrlEncoded
    @POST("delete_old.php")
    suspend fun deleteOldRecords(
        @Field("days") days: Int
    ): Response<ApiResponse>
    // Die aktualisierte GET-Methode mit dem optionalen count-Parameter
    @GET("locations_count.php")
    suspend fun getLocationsCount(
        @Query("count") count: Int? = null
    ): Response<FetchLocationsResponse>
}
