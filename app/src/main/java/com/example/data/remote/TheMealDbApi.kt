package com.example.data.remote

import com.example.data.model.MealLookupResponse
import com.example.data.model.MealsFilterResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {
    @GET("api/json/v1/1/filter.php")
    suspend fun filterByIngredient(@Query("i") ingredient: String): MealsFilterResponse

    @GET("api/json/v1/1/lookup.php")
    suspend fun lookupMealById(@Query("i") idMeal: String): MealLookupResponse

    @GET("api/json/v1/1/search.php")
    suspend fun searchMeals(@Query("s") query: String): MealLookupResponse
}
