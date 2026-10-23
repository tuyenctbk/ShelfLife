package com.example.data.repository

import com.example.data.local.PantryDao
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class PantryRepository(private val pantryDao: PantryDao) {

    val activeItems: Flow<List<PantryItem>> = pantryDao.getActiveItems()
    val consumedItems: Flow<List<PantryItem>> = pantryDao.getConsumedItems()
    val wastedItems: Flow<List<PantryItem>> = pantryDao.getWastedItems()

    fun getItemsByLocation(location: StorageLocation): Flow<List<PantryItem>> {
        return pantryDao.getActiveItemsByLocation(location.name)
    }

    fun getExpiringSoonItems(): Flow<List<PantryItem>> {
        val threeDaysAhead = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)
        return pantryDao.getItemsExpiringBefore(threeDaysAhead)
    }

    suspend fun insertItem(item: PantryItem): Long {
        return pantryDao.insertItem(item)
    }

    suspend fun updateItem(item: PantryItem) {
        pantryDao.updateItem(item)
    }

    suspend fun markAsConsumed(id: Long) {
        pantryDao.markAsConsumed(id)
    }

    suspend fun markAsWasted(id: Long) {
        pantryDao.markAsWasted(id)
    }

    suspend fun deleteItem(id: Long) {
        pantryDao.deleteItemById(id)
    }

    suspend fun seedSampleDataIfEmpty() {
        if (pantryDao.getItemCount() == 0) {
            val now = System.currentTimeMillis()
            val samples = listOf(
                PantryItem(
                    name = "Whole Milk",
                    brand = "Horizon Organic",
                    category = "Dairy",
                    quantity = 1.0,
                    unit = "gal",
                    storageLocation = StorageLocation.FRIDGE.name,
                    purchaseDateMillis = now - TimeUnit.DAYS.toMillis(5),
                    expiryDateMillis = now + TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(8), // Expiring in ~1 day!
                    barcode = "0074236526448",
                    imageUrl = "https://images.openfoodfacts.org/images/products/007/423/652/6448/front_en.11.400.jpg",
                    ingredientsText = "Organic Grade A milk, Vitamin D3",
                    nutriScore = "B",
                    caloriesPer100g = 64.0,
                    notes = "Great for smoothies, pancakes, or cheese sauce"
                ),
                PantryItem(
                    name = "Baby Spinach",
                    brand = "Organic Girl",
                    category = "Produce",
                    quantity = 1.0,
                    unit = "tub (5oz)",
                    storageLocation = StorageLocation.FRIDGE.name,
                    purchaseDateMillis = now - TimeUnit.DAYS.toMillis(4),
                    expiryDateMillis = now + TimeUnit.DAYS.toMillis(2), // Expiring in 2 days!
                    barcode = "858176002016",
                    imageUrl = "https://images.openfoodfacts.org/images/products/085/817/600/2016/front_en.4.400.jpg",
                    ingredientsText = "Baby Spinach",
                    nutriScore = "A",
                    caloriesPer100g = 23.0,
                    notes = "Needs to be cooked or blended into pasta sauce soon"
                ),
                PantryItem(
                    name = "Penne Rigate",
                    brand = "Barilla",
                    category = "Pantry",
                    quantity = 2.0,
                    unit = "boxes (16oz)",
                    storageLocation = StorageLocation.PANTRY.name,
                    purchaseDateMillis = now - TimeUnit.DAYS.toMillis(10),
                    expiryDateMillis = now + TimeUnit.DAYS.toMillis(180),
                    barcode = "8076809513753",
                    imageUrl = "https://images.openfoodfacts.org/images/products/807/680/951/3753/front_en.88.400.jpg",
                    ingredientsText = "Semolina durum wheat flour",
                    nutriScore = "A",
                    caloriesPer100g = 359.0,
                    notes = "Shelf staple"
                ),
                PantryItem(
                    name = "Chicken Breast",
                    brand = "Bell & Evans",
                    category = "Meat",
                    quantity = 1.5,
                    unit = "lbs",
                    storageLocation = StorageLocation.FRIDGE.name,
                    purchaseDateMillis = now - TimeUnit.DAYS.toMillis(1),
                    expiryDateMillis = now + TimeUnit.DAYS.toMillis(3), // Expiring in 3 days!
                    barcode = "0078954000101",
                    imageUrl = "https://images.openfoodfacts.org/images/products/007/895/400/0101/front_en.4.400.jpg",
                    ingredientsText = "Boneless skinless chicken breast",
                    nutriScore = "A",
                    caloriesPer100g = 165.0,
                    notes = "Cook within 3 days or freeze"
                ),
                PantryItem(
                    name = "Frozen Blueberries",
                    brand = "Wyman's",
                    category = "Frozen",
                    quantity = 1.0,
                    unit = "bag (3lb)",
                    storageLocation = StorageLocation.FREEZER.name,
                    purchaseDateMillis = now - TimeUnit.DAYS.toMillis(20),
                    expiryDateMillis = now + TimeUnit.DAYS.toMillis(90),
                    barcode = "0071114002010",
                    imageUrl = "https://images.openfoodfacts.org/images/products/007/111/400/2010/front_en.16.400.jpg",
                    ingredientsText = "Wild blueberries",
                    nutriScore = "A",
                    caloriesPer100g = 57.0,
                    notes = "Good for breakfast bowls"
                )
            )
            pantryDao.insertItems(samples)
        }
    }
}
