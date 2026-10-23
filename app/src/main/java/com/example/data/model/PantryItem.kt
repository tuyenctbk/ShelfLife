package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val barcode: String = "",
    val name: String,
    val brand: String = "",
    val category: String = "Pantry",
    val quantity: Double = 1.0,
    val unit: String = "item",
    val storageLocation: String = StorageLocation.PANTRY.name,
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val expiryDateMillis: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7),
    val imageUrl: String? = null,
    val ingredientsText: String? = null,
    val nutriScore: String? = null,
    val caloriesPer100g: Double? = null,
    val isConsumed: Boolean = false,
    val isWasted: Boolean = false,
    val notes: String? = null
) {
    fun daysUntilExpiry(): Int {
        val diff = expiryDateMillis - System.currentTimeMillis()
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return days.toInt()
    }

    fun isExpiringSoon(): Boolean {
        if (isConsumed || isWasted) return false
        val days = daysUntilExpiry()
        return days in 0..3
    }

    fun isExpired(): Boolean {
        if (isConsumed || isWasted) return false
        return daysUntilExpiry() < 0
    }
}
