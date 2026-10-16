package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InventoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    /**
     * Query inventory items sorted ascending by expiration date (FEFO logic).
     * Items closest to their expiration date are retrieved first.
     */
    @Query("SELECT * FROM inventory_items ORDER BY expiryDate ASC")
    fun getItemsSortedByExpiryDate(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items ORDER BY expiryDate ASC")
    suspend fun getItemsSortedByExpiryDateSync(): List<InventoryItem>

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE category = :category ORDER BY expiryDate ASC")
    fun getItemsByCategory(category: String): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE quantity <= :threshold ORDER BY expiryDate ASC")
    fun getLowStockItems(threshold: Int = 10): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR batchNumber LIKE '%' || :query || '%' ORDER BY expiryDate ASC")
    fun searchItems(query: String): Flow<List<InventoryItem>>

    // CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>): List<Long>

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
}
