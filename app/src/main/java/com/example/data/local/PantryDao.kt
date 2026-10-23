package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PantryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items WHERE isConsumed = 0 AND isWasted = 0 ORDER BY expiryDateMillis ASC")
    fun getActiveItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 0 AND isWasted = 0 AND storageLocation = :location ORDER BY expiryDateMillis ASC")
    fun getActiveItemsByLocation(location: String): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 0 AND isWasted = 0 AND expiryDateMillis <= :thresholdMillis ORDER BY expiryDateMillis ASC")
    fun getItemsExpiringBefore(thresholdMillis: Long): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 1 ORDER BY expiryDateMillis DESC")
    fun getConsumedItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isWasted = 1 ORDER BY expiryDateMillis DESC")
    fun getWastedItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE id = :id LIMIT 1")
    fun getItemById(id: Long): Flow<PantryItem?>

    @Query("SELECT * FROM pantry_items WHERE barcode = :barcode AND isConsumed = 0 AND isWasted = 0 LIMIT 1")
    suspend fun findActiveByBarcode(barcode: String): PantryItem?

    @Query("SELECT COUNT(*) FROM pantry_items")
    suspend fun getItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PantryItem>)

    @Update
    suspend fun updateItem(item: PantryItem)

    @Query("UPDATE pantry_items SET isConsumed = 1, isWasted = 0 WHERE id = :id")
    suspend fun markAsConsumed(id: Long)

    @Query("UPDATE pantry_items SET isWasted = 1, isConsumed = 0 WHERE id = :id")
    suspend fun markAsWasted(id: Long)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM pantry_items")
    suspend fun clearAll()
}
