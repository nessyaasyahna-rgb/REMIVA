package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["expiryDate"]),
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String,
    val batchNumber: String,
    val expiryDate: String, // YYYY-MM-DD for FEFO (First Expired, First Out) sorting
    val quantity: Int,
    val price: Double,
    val category: String, // e.g. "Obat Bebas", "Obat Keras", "Psikotropika", "Narkotika", "Suplemen", "Alkes"
    val unit: String = "Strip",
    val minStockThreshold: Int = 10,
    val lastModified: Long = System.currentTimeMillis()
)
