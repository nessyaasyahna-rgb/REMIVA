package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import com.example.data.model.DrugCategory
import com.example.data.model.DrugItem
import com.example.data.model.DrugWithBatchesRelation
import com.example.data.model.ExpenseItem
import com.example.data.model.OfflineSyncQueueItem
import com.example.data.model.StockBatch
import com.example.data.model.StockMutation
import com.example.data.model.StockOpnameRecord
import com.example.data.model.Transaction
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugDao {
    @Query("SELECT * FROM drugs WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllDrugs(): Flow<List<DrugItem>>

    @Query("SELECT * FROM drugs WHERE isDeleted = 0")
    suspend fun getAllDrugsSync(): List<DrugItem>

    @RoomTransaction
    @Query("SELECT * FROM drugs WHERE isDeleted = 0 ORDER BY name ASC")
    fun getDrugsWithBatchesRelation(): Flow<List<DrugWithBatchesRelation>>

    @Query("SELECT * FROM drugs WHERE id = :id")
    suspend fun getDrugById(id: Long): DrugItem?

    @Query("SELECT * FROM drugs WHERE barcode = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun getDrugByBarcode(barcode: String): DrugItem?

    @Query("SELECT * FROM drugs WHERE LOWER(name) = LOWER(:name) AND isDeleted = 0 LIMIT 1")
    suspend fun getDrugByName(name: String): DrugItem?

    @Query("SELECT * FROM drugs WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR genericName LIKE '%' || :query || '%' OR indication LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchDrugs(query: String): Flow<List<DrugItem>>

    @Query("SELECT * FROM drugs WHERE isDeleted = 0 AND category = :category ORDER BY name ASC")
    fun getDrugsByCategory(category: DrugCategory): Flow<List<DrugItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrug(drug: DrugItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrugs(drugs: List<DrugItem>): List<Long>

    @Update
    suspend fun updateDrug(drug: DrugItem)

    @Query("UPDATE drugs SET isDeleted = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteDrug(id: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteDrug(drug: DrugItem)
}

@Dao
interface StockBatchDao {
    @Query("SELECT * FROM stock_batches ORDER BY expiryDate ASC")
    fun getAllBatches(): Flow<List<StockBatch>>

    @Query("SELECT * FROM stock_batches ORDER BY expiryDate ASC")
    suspend fun getAllBatchesSync(): List<StockBatch>

    @Query("SELECT * FROM stock_batches WHERE drugId = :drugId ORDER BY expiryDate ASC")
    fun getBatchesForDrug(drugId: Long): Flow<List<StockBatch>>

    @Query("SELECT * FROM stock_batches WHERE drugId = :drugId ORDER BY expiryDate ASC")
    suspend fun getBatchesForDrugSync(drugId: Long): List<StockBatch>

    @Query("SELECT * FROM stock_batches WHERE id = :batchId")
    suspend fun getBatchById(batchId: Long): StockBatch?

    @Query("SELECT * FROM stock_batches WHERE expiryDate <= :cutoffDate ORDER BY expiryDate ASC")
    fun getExpiringBatches(cutoffDate: String): Flow<List<StockBatch>>

    @Query("SELECT * FROM stock_batches WHERE gudangStock > 0 ORDER BY batchNumber ASC")
    fun getWarehouseBatches(): Flow<List<StockBatch>>

    @Query("SELECT * FROM stock_batches WHERE etalaseStock > 0 ORDER BY expiryDate ASC")
    fun getShelfBatches(): Flow<List<StockBatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: StockBatch): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<StockBatch>): List<Long>

    @Update
    suspend fun updateBatch(batch: StockBatch)

    @Delete
    suspend fun deleteBatch(batch: StockBatch)

    @Query("UPDATE stock_batches SET etalaseStock = etalaseStock - :deductQty, lastModified = :timestamp WHERE id = :batchId")
    suspend fun deductEtalaseStock(batchId: Long, deductQty: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE stock_batches SET gudangStock = gudangStock - :qty, etalaseStock = etalaseStock + :qty, lastModified = :timestamp WHERE id = :batchId")
    suspend fun transferWarehouseToShelf(batchId: Long, qty: Int, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isPrescription = 1 ORDER BY timestamp DESC")
    fun getPrescriptionTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT SUM(grandTotal) FROM transactions")
    fun getTotalRevenue(): Flow<Double?>
}

@Dao
interface StockMutationDao {
    @Query("SELECT * FROM stock_mutations ORDER BY timestamp DESC")
    fun getAllMutations(): Flow<List<StockMutation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutation(mutation: StockMutation): Long
}

@Dao
interface StockOpnameDao {
    @Query("SELECT * FROM stock_opname_records ORDER BY timestamp DESC")
    fun getAllOpnameRecords(): Flow<List<StockOpnameRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpnameRecord(record: StockOpnameRecord): Long
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseItem): Long

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenses(): Flow<Double?>
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user_accounts ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserAccount>)

    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccount?
}

@Dao
interface OfflineSyncDao {
    @Query("SELECT * FROM offline_sync_queue WHERE status = 'QUEUED' ORDER BY createdAt ASC")
    fun getPendingSyncQueue(): Flow<List<OfflineSyncQueueItem>>

    @Query("SELECT COUNT(*) FROM offline_sync_queue WHERE status = 'QUEUED'")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: OfflineSyncQueueItem): Long

    @Query("UPDATE offline_sync_queue SET status = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE offline_sync_queue SET status = 'SYNCED'")
    suspend fun markAllSynced()

    @Query("DELETE FROM offline_sync_queue WHERE status = 'SYNCED'")
    suspend fun clearCompleted()
}
