package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenFoodFactsResponse(
    @Json(name = "status") val status: Int? = 0,
    @Json(name = "code") val code: String? = null,
    @Json(name = "product") val product: OffProduct? = null
)

@JsonClass(generateAdapter = true)
data class OffProduct(
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @Json(name = "categories") val categories: String? = null,
    @Json(name = "ingredients_text") val ingredientsText: String? = null,
    @Json(name = "nutriscore_grade") val nutriScoreGrade: String? = null,
    @Json(name = "nutriments") val nutriments: OffNutriments? = null
)

@JsonClass(generateAdapter = true)
data class OffNutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null,
    @Json(name = "fiber_100g") val fiber100g: Double? = null,
    @Json(name = "sodium_100g") val sodium100g: Double? = null
)
