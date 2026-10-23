package com.example.data.remote

import com.example.data.model.OpenFoodFactsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Header("User-Agent") userAgent: String = "ShelfLifeAndroidApp - Version 1.0 - contact: shelflife@example.com"
    ): OpenFoodFactsResponse
}
