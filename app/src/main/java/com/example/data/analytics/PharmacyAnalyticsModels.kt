package com.example.data.analytics

data class DailySalesMetric(
    val dateLabel: String,      // e.g. "05 Okt"
    val fullDate: String,       // e.g. "5 Oktober 2026"
    val salesAmount: Double,    // e.g. 2850000.0
    val cogsAmount: Double,     // Cost of Goods Sold (HPP)
    val grossProfit: Double,    // salesAmount - cogsAmount
    val txCount: Int,           // Total struk transaksi
    val avgTicket: Double,      // Rata-rata belanja per struk
    val isToday: Boolean = false
)

data class MonthlySalesMetric(
    val monthLabel: String,     // e.g. "Jan", "Feb", ... "Okt"
    val monthName: String,      // e.g. "Oktober 2026"
    val salesAmount: Double,    // Omzet aktual
    val targetAmount: Double,   // Target penjualan
    val cogsAmount: Double,     // HPP
    val grossProfit: Double,    // Laba kotor
    val growthMoM: Double,      // Pertumbuhan vs bulan lalu (%)
    val txCount: Int            // Jumlah transaksi
) {
    val achievementPercent: Double get() = if (targetAmount > 0) (salesAmount / targetAmount) * 100.0 else 100.0
}

data class InventoryTurnoverMetric(
    val monthLabel: String,          // e.g. "Mei", "Jun", ... "Okt"
    val turnoverRatio: Double,       // ITR = COGS Annualized / Avg Inventory Value (e.g. 6.4x)
    val daysSalesOfInventory: Int,   // DSI = 365 / ITR (e.g. 38 hari)
    val avgInventoryVal: Double,     // Nilai rata-rata stok di etalase + gudang
    val cogsAmount: Double,          // HPP dalam periode
    val benchmarkMin: Double = 4.0,  // Batas aman minimum apotek (4.0x)
    val benchmarkMax: Double = 8.0,  // Batas atas efisiensi apotek (8.0x)
    val statusText: String           // "Optimal (Sangat Sehat)", "Normal", etc.
)

data class CategoryTurnoverMetric(
    val categoryName: String,
    val turnoverRatio: Double,       // e.g. 9.8x
    val daysInStock: Int,            // e.g. 24 hari
    val totalStockValue: Double,     // Rp total nilai modal
    val skuCount: Int,               // Jumlah SKU dalam kategori
    val velocityRating: String,      // "Fast Moving ⚡", "Moderate", "Slow Moving 🐢"
    val riskLevel: String            // "Rendah", "Sedang", "Perlu Perhatian"
)

data class PharmacyAnalyticsSummary(
    val todaySales: Double,
    val yesterdaySales: Double,
    val salesGrowthDaily: Double,
    val monthToDateSales: Double,
    val monthlyTarget: Double,
    val monthTargetAchievement: Double,
    val currentTurnoverRatio: Double,
    val currentDaysOfInventory: Int,
    val totalInventoryValuation: Double,
    val totalEtalaseValuation: Double,
    val totalGudangValuation: Double,
    val fastMovingStockRatio: Double,
    val deadStockRiskValue: Double
)
