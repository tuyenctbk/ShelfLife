package com.example.data.remote

import com.example.data.model.UsdaSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface UsdaFoodApi {
    @GET("fdc/v1/foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 3,
        @Query("api_key") apiKey: String = "DEMO_KEY"
    ): UsdaSearchResponse
}
