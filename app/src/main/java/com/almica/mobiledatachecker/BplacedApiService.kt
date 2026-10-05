package com.almica.mobiledatachecker

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

data class ApiResponse(
    val status: String,
    val message: String,
)
data class UploadResponse(val status: String, val message: String, val url: String?)
interface BplacedApiService {
    @FormUrlEncoded
    @POST("location.php")
    suspend fun saveLocation(
        @Field("title") title: String,
        @Field("image_url") imageUrl: String?,
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
    @Multipart
    @POST("location_with_photo.php")
    suspend fun uploadLocationWithPhoto(
        @Part("title") title: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>
}
