package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UsdaSearchResponse(
    @Json(name = "foods") val foods: List<UsdaFoodItem>? = null
)

@JsonClass(generateAdapter = true)
data class UsdaFoodItem(
    @Json(name = "fdcId") val fdcId: Long,
    @Json(name = "description") val description: String,
    @Json(name = "brandOwner") val brandOwner: String? = null,
    @Json(name = "foodNutrients") val foodNutrients: List<UsdaNutrient>? = null
)

@JsonClass(generateAdapter = true)
data class UsdaNutrient(
    @Json(name = "nutrientName") val nutrientName: String? = null,
    @Json(name = "value") val value: Double? = null,
    @Json(name = "unitName") val unitName: String? = null
)
