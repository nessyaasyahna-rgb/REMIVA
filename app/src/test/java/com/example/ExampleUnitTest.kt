package com.example

import com.example.data.model.CartItem
import com.example.data.model.DrugCategory
import com.example.data.model.DrugItem
import com.example.data.model.DrugWithBatches
import com.example.data.model.DrugWithBatchesRelation
import com.example.data.model.InventoryItem
import com.example.data.model.OfflineSyncQueueItem
import com.example.data.model.OpnameDiscrepancyItem
import com.example.data.model.StockBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testInventoryItemFefoSorting() {
        val item1 = InventoryItem(
            id = 1,
            name = "Paracetamol 500mg (Batch Lama)",
            barcode = "8999901001",
            batchNumber = "B-PCT2611",
            expiryDate = "2026-11-20",
            quantity = 25,
            price = 6500.0,
            category = "Obat Bebas"
        )
        val item2 = InventoryItem(
            id = 2,
            name = "Paracetamol 500mg (Batch Baru)",
            barcode = "8999901001",
            batchNumber = "B-PCT2705",
            expiryDate = "2027-05-15",
            quantity = 50,
            price = 6500.0,
            category = "Obat Bebas"
        )
        val item3 = InventoryItem(
            id = 3,
            name = "Amoxicillin 500mg",
            barcode = "8999901002",
            batchNumber = "B-AMX2612",
            expiryDate = "2026-12-10",
            quantity = 30,
            price = 12000.0,
            category = "Obat Keras"
        )

        val unsortedList = listOf(item2, item1, item3)
        // FEFO sorting: sorted by expiryDate ascending
        val fefoList = unsortedList.sortedBy { it.expiryDate }

        assertEquals("B-PCT2611", fefoList[0].batchNumber)
        assertEquals("2026-11-20", fefoList[0].expiryDate)
        assertEquals("B-AMX2612", fefoList[1].batchNumber)
        assertEquals("2026-12-10", fefoList[1].expiryDate)
        assertEquals("B-PCT2705", fefoList[2].batchNumber)
        assertEquals("2027-05-15", fefoList[2].expiryDate)

        // Verify all required fields
        assertEquals(1L, item1.id)
        assertEquals("Paracetamol 500mg (Batch Lama)", item1.name)
        assertEquals("8999901001", item1.barcode)
        assertEquals(25, item1.quantity)
        assertEquals(6500.0, item1.price, 0.001)
        assertEquals("Obat Bebas", item1.category)
    }

    @Test
    fun testFefoBatchSorting() {
        val drug = DrugItem(
            id = 1,
            barcode = "8999901001",
            name = "Paracetamol 500mg",
            genericName = "Paracetamol",
            category = DrugCategory.BEBAS,
            indication = "Demam",
            unit = "Strip",
            buyPrice = 3000.0,
            sellPrice = 5000.0
        )
        val batch1 = StockBatch(id = 1, drugId = 1, batchNumber = "B2", expiryDate = "2027-05-15", etalaseStock = 10, gudangStock = 20)
        val batch2 = StockBatch(id = 2, drugId = 1, batchNumber = "B1", expiryDate = "2026-11-20", etalaseStock = 5, gudangStock = 10)

        val item = DrugWithBatches(drug = drug, batches = listOf(batch1, batch2))

        // FEFO: nearest expiry date (2026-11-20) must be first
        assertEquals("B1", item.fefoBatches.first().batchNumber)
        assertEquals("2026-11-20", item.nearestExpiryDate)
        assertEquals(15, item.totalEtalase)
        assertEquals(30, item.totalGudang)
        assertEquals(45, item.totalStock)
    }

    @Test
    fun testDrugWithBatchesRelation() {
        val drug = DrugItem(
            id = 1,
            barcode = "8999901002",
            name = "Amoxicillin 500mg",
            genericName = "Amoxicillin",
            category = DrugCategory.KERAS,
            indication = "Antibiotik",
            unit = "Strip",
            buyPrice = 7000.0,
            sellPrice = 12000.0,
            minStockThreshold = 20
        )
        val batches = listOf(
            StockBatch(id = 1, drugId = 1, batchNumber = "B-AMX01", expiryDate = "2026-12-01", etalaseStock = 5, gudangStock = 10),
            StockBatch(id = 2, drugId = 1, batchNumber = "B-AMX02", expiryDate = "2027-10-01", etalaseStock = 8, gudangStock = 20)
        )
        val relation = DrugWithBatchesRelation(drug = drug, batches = batches)

        assertEquals("B-AMX01", relation.fefoBatches.first().batchNumber)
        assertEquals(13, relation.totalEtalase)
        assertEquals(30, relation.totalGudang)
        assertEquals(43, relation.totalStock)
        assertFalse(relation.isLowStock)
    }

    @Test
    fun testOfflineSyncQueueModel() {
        val queueItem = OfflineSyncQueueItem(
            id = 1,
            actionType = "CREATE_DRUG",
            entityId = 42,
            payloadSummary = "Obat: Cefixime 100mg",
            status = "QUEUED"
        )
        assertEquals("CREATE_DRUG", queueItem.actionType)
        assertEquals(42L, queueItem.entityId)
        assertEquals("QUEUED", queueItem.status)
    }

    @Test
    fun testCartCalculationAndDiscount() {
        val drug = DrugItem(
            id = 1,
            barcode = "8999901001",
            name = "Amoxicillin 500mg",
            genericName = "Amoxicillin",
            category = DrugCategory.KERAS,
            indication = "Antibiotik",
            unit = "Strip",
            buyPrice = 7000.0,
            sellPrice = 10000.0
        )
        val batch = StockBatch(id = 1, drugId = 1, batchNumber = "B-AMX", expiryDate = "2027-01-01", etalaseStock = 10, gudangStock = 20)
        val cartItem = CartItem(drug = drug, batch = batch, quantity = 3, itemDiscountPercent = 10.0)

        assertEquals(9000.0, cartItem.unitPrice, 0.001)
        assertEquals(27000.0, cartItem.lineTotal, 0.001)
    }

    @Test
    fun testStockOpnameLossValuation() {
        val drug = DrugItem(
            id = 1,
            barcode = "8999901001",
            name = "Paracetamol 500mg",
            genericName = "Paracetamol",
            category = DrugCategory.BEBAS,
            indication = "Demam",
            unit = "Strip",
            buyPrice = 4000.0,
            sellPrice = 7000.0
        )
        val batch = StockBatch(id = 1, drugId = 1, batchNumber = "B-PCT", expiryDate = "2027-01-01", etalaseStock = 10, gudangStock = 20)

        val opnameItem = OpnameDiscrepancyItem(
            drug = drug,
            batch = batch,
            physicalEtalase = 8,
            physicalGudang = 19
        )

        assertEquals(30, opnameItem.systemTotal)
        assertEquals(27, opnameItem.physicalTotal)
        assertEquals(-3, opnameItem.diffUnits)
        assertEquals(12000.0, opnameItem.lossValue, 0.001)
    }

    @Test
    fun testPharmacyAnalyticsEngineCalculations() {
        val daily = com.example.data.analytics.PharmacyAnalyticsEngine.computeDailyMetrics(emptyList(), daysCount = 7)
        assertEquals(7, daily.size)
        assertTrue(daily.all { it.salesAmount > 0 })
        assertTrue(daily.all { it.cogsAmount > 0 })

        val monthly = com.example.data.analytics.PharmacyAnalyticsEngine.computeMonthlyMetrics(emptyList())
        assertEquals(10, monthly.size)
        assertTrue(monthly.last().achievementPercent > 80.0)

        val turnover = com.example.data.analytics.PharmacyAnalyticsEngine.computeInventoryTurnoverMetrics(emptyList(), monthly)
        assertEquals(6, turnover.size)
        assertTrue(turnover.last().turnoverRatio >= 4.0) // Sehat benchmark
        assertTrue(turnover.last().daysSalesOfInventory in 20..60)
    }
}
