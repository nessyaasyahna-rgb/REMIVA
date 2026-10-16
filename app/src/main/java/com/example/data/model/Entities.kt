package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class DrugCategory(val displayName: String, val badgeColorHex: Long) {
    BEBAS("Obat Bebas", 0xFF10B981), // Green
    BEBAS_TERBATAS("Obat Bebas Terbatas", 0xFF3B82F6), // Blue
    KERAS("Obat Keras (G)", 0xFFEF4444), // Red
    PSIKOTROPIKA("Psikotropika", 0xFF8B5CF6), // Purple
    NARKOTIKA("Narkotika", 0xFFDC2626), // Dark Red
    SUPLEMEN("Suplemen & Vitamin", 0xFFF59E0B), // Orange
    ALKES("Alat Kesehatan", 0xFF0D9488) // Teal
}

@Entity(
    tableName = "drugs",
    indices = [
        Index(value = ["barcode"], unique = false),
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class DrugItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val genericName: String,
    val category: DrugCategory,
    val indication: String, // Khasiat
    val unit: String, // Tablet, Strip, Botol, Tube, Box, Ampul
    val buyPrice: Double,
    val sellPrice: Double,
    val minStockThreshold: Int = 10,
    val isFastMoving: Boolean = false,
    val requirePrescription: Boolean = false,
    // Offline-first sync metadata
    val lastModified: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED", // "SYNCED", "PENDING_SYNC"
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "stock_batches",
    indices = [
        Index(value = ["drugId"]),
        Index(value = ["expiryDate"]),
        Index(value = ["batchNumber"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DrugItem::class,
            parentColumns = ["id"],
            childColumns = ["drugId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StockBatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drugId: Long,
    val batchNumber: String,
    val expiryDate: String, // YYYY-MM-DD for FEFO sorting
    val etalaseStock: Int, // In storefront shelf
    val gudangStock: Int,  // In warehouse storage
    val supplierName: String = "PBF Kimia Farma",
    // Offline-first sync metadata
    val lastModified: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
) {
    val totalStock: Int get() = etalaseStock + gudangStock
}

// Room Relation representation for offline-first querying
data class DrugWithBatchesRelation(
    @Embedded val drug: DrugItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "drugId"
    )
    val batches: List<StockBatch>
) {
    val fefoBatches: List<StockBatch> get() = batches.sortedBy { it.expiryDate }
    val totalEtalase: Int get() = batches.sumOf { it.etalaseStock }
    val totalGudang: Int get() = batches.sumOf { it.gudangStock }
    val totalStock: Int get() = totalEtalase + totalGudang
    val nearestExpiryDate: String get() = fefoBatches.firstOrNull()?.expiryDate ?: "-"
    val isLowStock: Boolean get() = totalStock <= drug.minStockThreshold
}

@Entity(
    tableName = "offline_sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class OfflineSyncQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // "CREATE_DRUG", "UPDATE_BATCH", "CHECKOUT_TRANSACTION", "MUTATION", "STOCK_OPNAME"
    val entityId: Long,
    val payloadSummary: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncAttemptCount: Int = 0,
    val status: String = "QUEUED" // "QUEUED", "SYNCED", "FAILED"
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["invoiceNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long,
    val cashierName: String,
    val customerName: String,
    val itemsSummary: String, // JSON or formatted summary of items
    val subtotal: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val paymentMethod: String, // "CASH", "QRIS", "DANA", "DEBIT"
    val cashGiven: Double,
    val changeAmount: Double,
    val isPrescription: Boolean = false,
    val doctorName: String = "",
    val prescriptionNumber: String = "",
    val syncStatus: String = "SYNCED"
)

@Entity(
    tableName = "stock_mutations",
    indices = [
        Index(value = ["drugId"]),
        Index(value = ["timestamp"])
    ]
)
data class StockMutation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drugId: Long,
    val drugName: String,
    val batchNumber: String,
    val fromLocation: String, // "GUDANG"
    val toLocation: String,   // "ETALASE"
    val quantity: Int,
    val timestamp: Long,
    val note: String,
    val operatorName: String,
    val syncStatus: String = "SYNCED"
)

@Entity(
    tableName = "stock_opname_records",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class StockOpnameRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateFormatted: String,
    val timestamp: Long,
    val auditorName: String,
    val totalDiscrepancyUnits: Int,
    val totalLossValuation: Double,
    val status: String, // "COMPLETED"
    val notes: String,
    val syncStatus: String = "SYNCED"
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val category: String, // "Operasional", "Gaji", "Listrik & Air", "Pembelian PBF", "Lainnya"
    val description: String,
    val amount: Double,
    val recordedBy: String,
    val syncStatus: String = "SYNCED"
)

enum class UserRole(val roleTitle: String) {
    ADMIN("Administrator"),
    APOTEKER("Apoteker (APA)"),
    TTK("Tenaga Teknis Kefarmasian"),
    KASIR("Kasir Apotek")
}

@Entity(
    tableName = "user_accounts",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val phone: String = ""
)
