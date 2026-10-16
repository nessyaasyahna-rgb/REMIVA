package com.example.data.model

data class DrugWithBatches(
    val drug: DrugItem,
    val batches: List<StockBatch>
) {
    // FEFO: batches sorted ascending by expiryDate (nearest expiry date first)
    val fefoBatches: List<StockBatch> get() = batches.sortedBy { it.expiryDate }
    val totalEtalase: Int get() = batches.sumOf { it.etalaseStock }
    val totalGudang: Int get() = batches.sumOf { it.gudangStock }
    val totalStock: Int get() = totalEtalase + totalGudang
    val nearestExpiryDate: String get() = fefoBatches.firstOrNull()?.expiryDate ?: "-"
    val isLowStock: Boolean get() = totalStock <= drug.minStockThreshold
}

data class CartItem(
    val drug: DrugItem,
    val batch: StockBatch,
    val quantity: Int,
    val itemDiscountPercent: Double = 0.0
) {
    val unitPrice: Double get() = drug.sellPrice * (1.0 - itemDiscountPercent / 100.0)
    val lineTotal: Double get() = unitPrice * quantity
}

data class PharmacyProfile(
    val pharmacyName: String = "APOTEK FARMIVA SEHAT",
    val address: String = "Jl. Farmasi Raya No. 45, Kebayoran Baru, Jakarta Selatan",
    val phone: String = "+62 812-8899-7700",
    val sipaNumber: String = "SIPA. 446/092/DINKES/2024",
    val siaNumber: String = "SIA. 503/120/SIA/BPPTSP/2023",
    val apotekerName: String = "apt. Sarah Olivia, S.Farm",
    val ttkName: String = "Budi Santoso, A.Md.Farm",
    val psaName: String = "H. Hendra Pratama, S.E.",
    val bankName: String = "Bank Central Asia (BCA)",
    val bankAccountNumber: String = "8220-4591-88",
    val bankAccountHolder: String = "Apotek Farmiva Sehat",
    val danaNumber: String = "0812-8899-7700",
    val qrisMidtransEnabled: Boolean = true,
    val cashierAudioEnabled: Boolean = true,
    val stockAlertEnabled: Boolean = true,
    val printerPaperWidth: String = "58mm", // "58mm" or "80mm"
    val autoBackupFrequency: String = "Harian", // "Harian", "Mingguan", "Off"
    val cloudSyncActive: Boolean = true
)

data class ManualOpnameAdjustment(
    val batchId: Long,
    val manualDiff: Int, // e.g. -3, +5
    val targetLocation: String = "PROPORSIONAL", // "GUDANG", "ETALASE", "PROPORSIONAL"
    val reason: String = "Penyesuaian Fisik Rutin",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class OpnameDiscrepancyItem(
    val drug: DrugItem,
    val batch: StockBatch,
    var physicalEtalase: Int,
    var physicalGudang: Int,
    var manualAdjustment: ManualOpnameAdjustment? = null
) {
    val systemTotal: Int get() = batch.etalaseStock + batch.gudangStock
    val physicalTotal: Int get() = physicalEtalase + physicalGudang
    val diffUnits: Int get() = physicalTotal - systemTotal
    val lossValue: Double get() = if (diffUnits < 0) -diffUnits * drug.buyPrice else 0.0
    val gainValue: Double get() = if (diffUnits > 0) diffUnits * drug.buyPrice else 0.0
    val systemValuation: Double get() = systemTotal * drug.buyPrice
    val physicalValuation: Double get() = physicalTotal * drug.buyPrice
    val valuationDiff: Double get() = physicalValuation - systemValuation
    val isManuallyAdjusted: Boolean get() = manualAdjustment != null
}
