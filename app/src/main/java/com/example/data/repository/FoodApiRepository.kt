package com.example.data.repository

import com.example.data.model.MealRecipe
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FoodApiRepository {

    suspend fun fetchProductByBarcode(barcode: String): Result<PantryItem> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.openFoodFactsApi.getProductByBarcode(barcode)
            val product = response.product
            if (product != null && (!product.productName.isNullOrBlank() || !product.brands.isNullOrBlank())) {
                val name = product.productName ?: product.brands ?: "Unknown Grocery Item"
                val brand = product.brands ?: ""
                val category = guessCategory(product.categories, name)
                val location = guessStorageLocation(category)
                val defaultExpiryDays = getDefaultExpiryDays(category, location)
                val now = System.currentTimeMillis()

                val item = PantryItem(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    category = category,
                    storageLocation = location.name,
                    purchaseDateMillis = now,
                    expiryDateMillis = now + TimeUnit.DAYS.toMillis(defaultExpiryDays),
                    imageUrl = product.imageFrontUrl ?: product.imageUrl,
                    ingredientsText = product.ingredientsText,
                    nutriScore = product.nutriScoreGrade?.uppercase(),
                    caloriesPer100g = product.nutriments?.energyKcal100g
                )
                Result.success(item)
            } else {
                // If OpenFoodFacts didn't have full data, fallback to USDA search
                val usdaResult = trySearchUsda(barcode)
                if (usdaResult != null) {
                    Result.success(usdaResult)
                } else {
                    Result.failure(Exception("Product not found in OpenFoodFacts database. You can still enter details manually!"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun trySearchUsda(query: String): PantryItem? {
        return try {
            val res = NetworkClient.usdaFoodApi.searchFoods(query)
            val food = res.foods?.firstOrNull() ?: return null
            val now = System.currentTimeMillis()
            PantryItem(
                barcode = query,
                name = food.description,
                brand = food.brandOwner ?: "",
                category = "Pantry",
                storageLocation = StorageLocation.PANTRY.name,
                purchaseDateMillis = now,
                expiryDateMillis = now + TimeUnit.DAYS.toMillis(14),
                caloriesPer100g = food.foodNutrients?.firstOrNull { it.nutrientName?.contains("Energy", true) == true }?.value
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchRecipesForExpiringItems(expiringItems: List<PantryItem>): List<MealRecipe> = withContext(Dispatchers.IO) {
        if (expiringItems.isEmpty()) return@withContext emptyList()

        val itemKeywords = expiringItems.map { cleanIngredientName(it.name) }.distinct()
        val allMealIdsWithMatchedItems = mutableMapOf<String, MutableSet<String>>()

        // Query TheMealDB for each ingredient keyword
        for (keyword in itemKeywords.take(4)) {
            try {
                val filterRes = NetworkClient.mealDbApi.filterByIngredient(keyword)
                val meals = filterRes.meals
                if (!meals.isNullOrEmpty()) {
                    for (meal in meals.take(6)) {
                        allMealIdsWithMatchedItems.getOrPut(meal.idMeal) { mutableSetOf() }.add(keyword)
                    }
                }
            } catch (_: Exception) {
                // Continue to next keyword
            }
        }

        // If no results from specific ingredients, search by common base queries
        if (allMealIdsWithMatchedItems.isEmpty()) {
            val fallbackKeywords = listOf("chicken", "pasta", "soup", "salad")
            for (fallback in fallbackKeywords) {
                try {
                    val searchRes = NetworkClient.mealDbApi.searchMeals(fallback)
                    searchRes.meals?.take(3)?.forEach { detail ->
                        allMealIdsWithMatchedItems.getOrPut(detail.idMeal) { mutableSetOf() }.add(fallback)
                    }
                    if (allMealIdsWithMatchedItems.size >= 4) break
                } catch (_: Exception) { }
            }
        }

        // Fetch full details for the top meals in parallel
        val topIds = allMealIdsWithMatchedItems.entries
            .sortedByDescending { it.value.size }
            .take(8)
            .map { it.key to it.value.toList() }

        val deferredDetails = topIds.map { (id, matchedList) ->
            async {
                try {
                    val lookup = NetworkClient.mealDbApi.lookupMealById(id)
                    val detail = lookup.meals?.firstOrNull()
                    detail?.toDomainModel(matchedList)
                } catch (_: Exception) {
                    null
                }
            }
        }

        deferredDetails.awaitAll().filterNotNull()
    }

    private fun cleanIngredientName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("milk") -> "milk"
            lower.contains("spinach") -> "spinach"
            lower.contains("chicken") -> "chicken"
            lower.contains("beef") -> "beef"
            lower.contains("pork") -> "pork"
            lower.contains("pasta") || lower.contains("penne") || lower.contains("spaghetti") -> "pasta"
            lower.contains("tomato") -> "tomato"
            lower.contains("egg") -> "egg"
            lower.contains("cheese") -> "cheese"
            lower.contains("potato") -> "potato"
            lower.contains("onion") -> "onion"
            lower.contains("garlic") -> "garlic"
            lower.contains("rice") -> "rice"
            lower.contains("salmon") || lower.contains("fish") -> "salmon"
            lower.contains("bread") -> "bread"
            lower.contains("butter") -> "butter"
            lower.contains("apple") -> "apple"
            lower.contains("banana") -> "banana"
            lower.contains("carrot") -> "carrot"
            lower.contains("mushroom") -> "mushroom"
            else -> lower.split(" ").firstOrNull() ?: lower
        }
    }

    private fun guessCategory(categoriesStr: String?, name: String): String {
        val combined = ((categoriesStr ?: "") + " " + name).lowercase()
        return when {
            combined.contains("milk") || combined.contains("cheese") || combined.contains("yogurt") || combined.contains("dairy") -> "Dairy"
            combined.contains("spinach") || combined.contains("apple") || combined.contains("lettuce") || combined.contains("produce") || combined.contains("fruit") || combined.contains("vegetable") -> "Produce"
            combined.contains("chicken") || combined.contains("beef") || combined.contains("meat") || combined.contains("fish") || combined.contains("salmon") || combined.contains("pork") -> "Meat"
            combined.contains("frozen") || combined.contains("ice cream") -> "Frozen"
            combined.contains("bread") || combined.contains("pastry") || combined.contains("bakery") || combined.contains("croissant") -> "Bakery"
            combined.contains("juice") || combined.contains("beverage") || combined.contains("tea") || combined.contains("coffee") || combined.contains("water") -> "Beverage"
            combined.contains("chip") || combined.contains("cookie") || combined.contains("snack") || combined.contains("chocolate") -> "Snacks"
            else -> "Pantry"
        }
    }

    private fun guessStorageLocation(category: String): StorageLocation {
        return when (category) {
            "Dairy", "Meat", "Produce" -> StorageLocation.FRIDGE
            "Frozen" -> StorageLocation.FREEZER
            else -> StorageLocation.PANTRY
        }
    }

    private fun getDefaultExpiryDays(category: String, location: StorageLocation): Long {
        if (location == StorageLocation.FREEZER) return 90L
        return when (category) {
            "Produce" -> 5L
            "Bakery" -> 4L
            "Dairy" -> 7L
            "Meat" -> 3L
            "Beverage" -> 21L
            "Snacks" -> 60L
            else -> 120L
        }
    }
}
