package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.DrugDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.InventoryDao
import com.example.data.dao.OfflineSyncDao
import com.example.data.dao.StockBatchDao
import com.example.data.dao.StockMutationDao
import com.example.data.dao.StockOpnameDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserDao
import com.example.data.model.DrugCategory
import com.example.data.model.DrugItem
import com.example.data.model.ExpenseItem
import com.example.data.model.InventoryItem
import com.example.data.model.OfflineSyncQueueItem
import com.example.data.model.StockBatch
import com.example.data.model.StockMutation
import com.example.data.model.StockOpnameRecord
import com.example.data.model.Transaction
import com.example.data.model.UserAccount
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromDrugCategory(category: DrugCategory): String = category.name

    @TypeConverter
    fun toDrugCategory(value: String): DrugCategory = runCatching {
        DrugCategory.valueOf(value)
    }.getOrDefault(DrugCategory.BEBAS)

    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching {
        UserRole.valueOf(value)
    }.getOrDefault(UserRole.KASIR)
}

@Database(
    entities = [
        DrugItem::class,
        StockBatch::class,
        Transaction::class,
        StockMutation::class,
        StockOpnameRecord::class,
        ExpenseItem::class,
        UserAccount::class,
        OfflineSyncQueueItem::class,
        InventoryItem::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FarmivaDatabase : RoomDatabase() {
    abstract fun drugDao(): DrugDao
    abstract fun stockBatchDao(): StockBatchDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockMutationDao(): StockMutationDao
    abstract fun stockOpnameDao(): StockOpnameDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun userDao(): UserDao
    abstract fun offlineSyncDao(): OfflineSyncDao

    companion object {
        @Volatile
        private var INSTANCE: FarmivaDatabase? = null

        fun getDatabase(context: Context): FarmivaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FarmivaDatabase::class.java,
                    "farmiva_pharmacy.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
