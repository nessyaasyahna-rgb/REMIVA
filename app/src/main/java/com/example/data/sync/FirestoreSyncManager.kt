package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.database.FarmivaDatabase
import com.example.data.model.DrugCategory
import com.example.data.model.DrugItem
import com.example.data.model.StockBatch
import com.example.data.model.StockMutation
import com.example.data.model.StockOpnameRecord
import com.example.data.model.Transaction
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val context: Context,
    private val db: FarmivaDatabase
) {
    private val TAG = "FirestoreSync"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firestore: FirebaseFirestore by lazy {
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private val _isCloudSynced = MutableStateFlow(false)
    val isCloudSynced: StateFlow<Boolean> = _isCloudSynced.asStateFlow()

    private val _syncStatusText = MutableStateFlow("Terhubung ke Cloud (Sinkronisasi Multi-Device Aktif)")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()
    private var isListening = false

    init {
        // Start real-time sync immediately for multi-device pharmacy operation
        startRealtimeSync()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _syncStatusText.value = "Terhubung: ${user.email ?: user.displayName ?: "Akun Cloud"} (Sync Aktif)"
                _isCloudSynced.value = true
            }
        }
    }

    fun startRealtimeSync() {
        if (isListening) return
        isListening = true
        _isCloudSynced.value = true
        _syncStatusText.value = "Terhubung ke Cloud (Sinkronisasi Multi-Device Aktif)"

        coroutineScope.launch {
            try {
                seedInitialDataIfFirestoreEmpty()
                attachListeners()
            } catch (e: Exception) {
                Log.w(TAG, "Notice on starting realtime sync: ${e.localizedMessage}")
                _syncStatusText.value = "Sinkronisasi Cloud: ${e.localizedMessage ?: "Menunggu koneksi"}"
            }
        }
    }

    fun stopRealtimeSync() {
        listeners.forEach { runCatching { it.remove() } }
        listeners.clear()
        isListening = false
        _isCloudSynced.value = false
    }

    private suspend fun seedInitialDataIfFirestoreEmpty() {
        try {
            val drugsSnapshot = firestore.collection("drugs").limit(1).get().await()
            if (drugsSnapshot.isEmpty) {
                Log.d(TAG, "Firestore drugs collection empty. Seeding initial catalog from local...")
                val localDrugs = db.drugDao().getDrugsWithBatchesRelation()
                // We'll read first list of drugs
                val sampleDrugs = listOf(
                    DrugItem(id = 1, barcode = "8999908123456", name = "Amoxicillin 500mg", genericName = "Amoxicillin Trihydrate", category = DrugCategory.KERAS, indication = "Antibiotik Infeksi Bakteri", unit = "Kaplet", buyPrice = 450.0, sellPrice = 850.0, minStockThreshold = 20, isFastMoving = true, requirePrescription = true),
                    DrugItem(id = 2, barcode = "8992753123456", name = "Paracetamol 500mg", genericName = "Paracetamol", category = DrugCategory.BEBAS, indication = "Pereda Demam & Nyeri", unit = "Strip", buyPrice = 2800.0, sellPrice = 4500.0, minStockThreshold = 15, isFastMoving = true, requirePrescription = false),
                    DrugItem(id = 3, barcode = "8992345678901", name = "Cetirizine 10mg", genericName = "Cetirizine HCl", category = DrugCategory.BEBAS_TERBATAS, indication = "Antihistamin / Alergi", unit = "Strip", buyPrice = 4000.0, sellPrice = 7500.0, minStockThreshold = 10, isFastMoving = false, requirePrescription = false),
                    DrugItem(id = 4, barcode = "8993456789012", name = "Vitamin C 500mg IPI", genericName = "Ascorbic Acid", category = DrugCategory.SUPLEMEN, indication = "Daya Tahan Tubuh", unit = "Botol", buyPrice = 6500.0, sellPrice = 9500.0, minStockThreshold = 10, isFastMoving = true, requirePrescription = false),
                    DrugItem(id = 5, barcode = "8994567890123", name = "Antasida Doen", genericName = "Al(OH)3 + Mg(OH)2", category = DrugCategory.BEBAS, indication = "Sakit Maag & Asam Lambung", unit = "Strip", buyPrice = 2200.0, sellPrice = 3500.0, minStockThreshold = 10, isFastMoving = true, requirePrescription = false)
                )

                for (drug in sampleDrugs) {
                    pushDrug(drug)
                }

                val sampleBatches = listOf(
                    StockBatch(id = 1, drugId = 1, batchNumber = "AMX-2026A", expiryDate = "2026-08-15", etalaseStock = 45, gudangStock = 120, supplierName = "PBF Kimia Farma"),
                    StockBatch(id = 2, drugId = 1, batchNumber = "AMX-2027B", expiryDate = "2027-02-20", etalaseStock = 0, gudangStock = 200, supplierName = "PBF Kimia Farma"),
                    StockBatch(id = 3, drugId = 2, batchNumber = "PCT-2026D", expiryDate = "2026-09-30", etalaseStock = 80, gudangStock = 150, supplierName = "PBF Mensa"),
                    StockBatch(id = 4, drugId = 3, batchNumber = "CTZ-2027A", expiryDate = "2027-05-10", etalaseStock = 35, gudangStock = 60, supplierName = "PBF Anugrah"),
                    StockBatch(id = 5, drugId = 4, batchNumber = "VTC-2026C", expiryDate = "2026-11-25", etalaseStock = 25, gudangStock = 40, supplierName = "PBF Kimia Farma"),
                    StockBatch(id = 6, drugId = 5, batchNumber = "ATD-2026B", expiryDate = "2026-07-20", etalaseStock = 50, gudangStock = 100, supplierName = "PBF Mensa")
                )

                for (batch in sampleBatches) {
                    pushBatch(batch)
                }

                val defaultUsers = listOf(
                    UserAccount(id = 1, username = "apoteker", email = "apoteker@farmiva.id", fullName = "apt. Sarah Olivia, S.Farm", role = UserRole.APOTEKER),
                    UserAccount(id = 2, username = "kasir", email = "kasir@farmiva.id", fullName = "Rian Pratama", role = UserRole.KASIR),
                    UserAccount(id = 3, username = "ttk", email = "ttk@farmiva.id", fullName = "Budi Santoso, A.Md.Farm", role = UserRole.TTK),
                    UserAccount(id = 4, username = "admin", email = "admin@farmiva.id", fullName = "Dewi Lestari", role = UserRole.ADMIN)
                )

                for (user in defaultUsers) {
                    pushUser(user)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding initial data to Firestore", e)
        }
    }

    private fun attachListeners() {
        // 1. DRUGS LISTENER
        val drugReg = firestore.collection("drugs").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Drugs listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                coroutineScope.launch {
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id.toLongOrNull() ?: continue
                            val barcode = doc.getString("barcode") ?: ""
                            val name = doc.getString("name") ?: ""
                            val genericName = doc.getString("genericName") ?: ""
                            val catStr = doc.getString("category") ?: "BEBAS"
                            val category = runCatching { DrugCategory.valueOf(catStr) }.getOrDefault(DrugCategory.BEBAS)
                            val indication = doc.getString("indication") ?: ""
                            val unit = doc.getString("unit") ?: "Strip"
                            val buyPrice = doc.getDouble("buyPrice") ?: 0.0
                            val sellPrice = doc.getDouble("sellPrice") ?: 0.0
                            val minStockThreshold = (doc.getLong("minStockThreshold") ?: 10L).toInt()
                            val isFastMoving = doc.getBoolean("isFastMoving") ?: false
                            val requirePrescription = doc.getBoolean("requirePrescription") ?: false
                            val isDeleted = doc.getBoolean("isDeleted") ?: false
                            val lastModified = doc.getLong("lastModified") ?: System.currentTimeMillis()

                            val item = DrugItem(
                                id = id,
                                barcode = barcode,
                                name = name,
                                genericName = genericName,
                                category = category,
                                indication = indication,
                                unit = unit,
                                buyPrice = buyPrice,
                                sellPrice = sellPrice,
                                minStockThreshold = minStockThreshold,
                                isFastMoving = isFastMoving,
                                requirePrescription = requirePrescription,
                                lastModified = lastModified,
                                syncStatus = "SYNCED",
                                isDeleted = isDeleted
                            )
                            db.drugDao().insertDrug(item)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing drug doc ${doc.id}", e)
                        }
                    }
                }
            }
        }
        listeners.add(drugReg)

        // 2. STOCK BATCHES LISTENER
        val batchReg = firestore.collection("stock_batches").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Batches listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                coroutineScope.launch {
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id.toLongOrNull() ?: continue
                            val drugId = doc.getString("drugId")?.toLongOrNull()
                                ?: (doc.getLong("drugId") ?: 0L)
                            val batchNumber = doc.getString("batchNumber") ?: ""
                            val expiryDate = doc.getString("expiryDate") ?: ""
                            val etalaseStock = (doc.getLong("etalaseStock") ?: 0L).toInt()
                            val gudangStock = (doc.getLong("gudangStock") ?: 0L).toInt()
                            val supplierName = doc.getString("supplierName") ?: "PBF Kimia Farma"
                            val lastModified = doc.getLong("lastModified") ?: System.currentTimeMillis()

                            val batch = StockBatch(
                                id = id,
                                drugId = drugId,
                                batchNumber = batchNumber,
                                expiryDate = expiryDate,
                                etalaseStock = etalaseStock,
                                gudangStock = gudangStock,
                                supplierName = supplierName,
                                lastModified = lastModified,
                                syncStatus = "SYNCED"
                            )
                            db.stockBatchDao().insertBatch(batch)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing batch doc ${doc.id}", e)
                        }
                    }
                }
            }
        }
        listeners.add(batchReg)

        // 3. TRANSACTIONS LISTENER
        val txReg = firestore.collection("transactions").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Transactions listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                coroutineScope.launch {
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
                            val invoiceNumber = doc.getString("invoiceNumber") ?: doc.id
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val cashierName = doc.getString("cashierName") ?: "Kasir"
                            val customerName = doc.getString("customerName") ?: "Pelanggan Umum"
                            val itemsSummary = doc.getString("itemsSummary") ?: ""
                            val subtotal = doc.getDouble("subtotal") ?: 0.0
                            val discountPercent = doc.getDouble("discountPercent") ?: 0.0
                            val discountAmount = doc.getDouble("discountAmount") ?: 0.0
                            val grandTotal = doc.getDouble("grandTotal") ?: 0.0
                            val paymentMethod = doc.getString("paymentMethod") ?: "CASH"
                            val cashGiven = doc.getDouble("cashGiven") ?: grandTotal
                            val changeAmount = doc.getDouble("changeAmount") ?: 0.0
                            val isPrescription = doc.getBoolean("isPrescription") ?: false
                            val doctorName = doc.getString("doctorName") ?: ""
                            val prescriptionNumber = doc.getString("prescriptionNumber") ?: ""

                            val tx = Transaction(
                                id = id,
                                invoiceNumber = invoiceNumber,
                                timestamp = timestamp,
                                cashierName = cashierName,
                                customerName = customerName,
                                itemsSummary = itemsSummary,
                                subtotal = subtotal,
                                discountPercent = discountPercent,
                                discountAmount = discountAmount,
                                grandTotal = grandTotal,
                                paymentMethod = paymentMethod,
                                cashGiven = cashGiven,
                                changeAmount = changeAmount,
                                isPrescription = isPrescription,
                                doctorName = doctorName,
                                prescriptionNumber = prescriptionNumber,
                                syncStatus = "SYNCED"
                            )
                            db.transactionDao().insertTransaction(tx)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing transaction doc ${doc.id}", e)
                        }
                    }
                }
            }
        }
        listeners.add(txReg)

        // 4. MUTATIONS LISTENER
        val mutReg = firestore.collection("stock_mutations").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Mutations listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                coroutineScope.launch {
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id.toLongOrNull() ?: continue
                            val drugId = doc.getString("drugId")?.toLongOrNull() ?: (doc.getLong("drugId") ?: 0L)
                            val drugName = doc.getString("drugName") ?: ""
                            val batchNumber = doc.getString("batchNumber") ?: ""
                            val fromLoc = doc.getString("fromLocation") ?: "GUDANG"
                            val toLoc = doc.getString("toLocation") ?: "ETALASE"
                            val quantity = (doc.getLong("quantity") ?: 0L).toInt()
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val note = doc.getString("note") ?: ""
                            val operatorName = doc.getString("operatorName") ?: "Staff"

                            val mutation = StockMutation(
                                id = id,
                                drugId = drugId,
                                drugName = drugName,
                                batchNumber = batchNumber,
                                fromLocation = fromLoc,
                                toLocation = toLoc,
                                quantity = quantity,
                                timestamp = timestamp,
                                note = note,
                                operatorName = operatorName,
                                syncStatus = "SYNCED"
                            )
                            db.stockMutationDao().insertMutation(mutation)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing mutation doc ${doc.id}", e)
                        }
                    }
                }
            }
        }
        listeners.add(mutReg)

        // 5. STOCK OPNAME LISTENER
        val opnameReg = firestore.collection("stock_opname_records").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Stock Opname listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                coroutineScope.launch {
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id.toLongOrNull() ?: continue
                            val dateFormatted = doc.getString("dateFormatted") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val auditorName = doc.getString("auditorName") ?: "Auditor"
                            val totalDiscrepancyUnits = (doc.getLong("totalDiscrepancyUnits") ?: 0L).toInt()
                            val totalLossValuation = doc.getDouble("totalLossValuation") ?: 0.0
                            val status = doc.getString("status") ?: "COMPLETED"
                            val notes = doc.getString("notes") ?: ""

                            val opname = StockOpnameRecord(
                                id = id,
                                dateFormatted = dateFormatted,
                                timestamp = timestamp,
                                auditorName = auditorName,
                                totalDiscrepancyUnits = totalDiscrepancyUnits,
                                totalLossValuation = totalLossValuation,
                                status = status,
                                notes = notes,
                                syncStatus = "SYNCED"
                            )
                            db.stockOpnameDao().insertOpnameRecord(opname)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing stock opname doc ${doc.id}", e)
                        }
                    }
                }
            }
        }
        listeners.add(opnameReg)

        // 6. USER ACCOUNTS LISTENER
        val userReg = firestore.collection("user_accounts").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "User accounts listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                coroutineScope.launch {
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
                            val username = doc.getString("username") ?: doc.id
                            val email = doc.getString("email") ?: "${username}@farmiva.id"
                            val fullName = doc.getString("fullName") ?: username
                            val roleStr = doc.getString("role") ?: "APOTEKER"
                            val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.APOTEKER)
                            val phone = doc.getString("phone") ?: ""

                            val user = UserAccount(
                                id = id,
                                username = username,
                                email = email,
                                fullName = fullName,
                                role = role,
                                phone = phone
                            )
                            db.userDao().insertUser(user)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing user doc ${doc.id}", e)
                        }
                    }
                }
            }
        }
        listeners.add(userReg)
    }

    // ========================================================
    // OUTBOUND PUSH METHODS TO FIRESTORE (WRITE & DELETE)
    // ========================================================

    fun pushDrug(drug: DrugItem) {
        coroutineScope.launch {
            try {
                val data = hashMapOf(
                    "barcode" to drug.barcode,
                    "name" to drug.name,
                    "genericName" to drug.genericName,
                    "category" to drug.category.name,
                    "indication" to drug.indication,
                    "unit" to drug.unit,
                    "buyPrice" to drug.buyPrice,
                    "sellPrice" to drug.sellPrice,
                    "minStockThreshold" to drug.minStockThreshold,
                    "isFastMoving" to drug.isFastMoving,
                    "requirePrescription" to drug.requirePrescription,
                    "isDeleted" to drug.isDeleted,
                    "lastModified" to drug.lastModified
                )
                firestore.collection("drugs").document(drug.id.toString()).set(data).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing drug ${drug.name} to Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun deleteDrug(drugId: Long) {
        coroutineScope.launch {
            try {
                val updateData = hashMapOf<String, Any>(
                    "isDeleted" to true,
                    "lastModified" to System.currentTimeMillis()
                )
                firestore.collection("drugs").document(drugId.toString()).update(updateData).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting drug $drugId from Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun pushBatch(batch: StockBatch) {
        coroutineScope.launch {
            try {
                val data = hashMapOf(
                    "drugId" to batch.drugId.toString(),
                    "batchNumber" to batch.batchNumber,
                    "expiryDate" to batch.expiryDate,
                    "etalaseStock" to batch.etalaseStock,
                    "gudangStock" to batch.gudangStock,
                    "supplierName" to batch.supplierName,
                    "lastModified" to batch.lastModified
                )
                firestore.collection("stock_batches").document(batch.id.toString()).set(data).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing batch ${batch.batchNumber} to Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun deleteBatch(batchId: Long) {
        coroutineScope.launch {
            try {
                firestore.collection("stock_batches").document(batchId.toString()).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting batch $batchId from Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun pushTransaction(tx: Transaction) {
        coroutineScope.launch {
            try {
                val data = hashMapOf(
                    "invoiceNumber" to tx.invoiceNumber,
                    "timestamp" to tx.timestamp,
                    "cashierName" to tx.cashierName,
                    "customerName" to tx.customerName,
                    "itemsSummary" to tx.itemsSummary,
                    "subtotal" to tx.subtotal,
                    "discountPercent" to tx.discountPercent,
                    "discountAmount" to tx.discountAmount,
                    "grandTotal" to tx.grandTotal,
                    "paymentMethod" to tx.paymentMethod,
                    "cashGiven" to tx.cashGiven,
                    "changeAmount" to tx.changeAmount,
                    "isPrescription" to tx.isPrescription,
                    "doctorName" to (tx.doctorName ?: ""),
                    "prescriptionNumber" to (tx.prescriptionNumber ?: "")
                )
                firestore.collection("transactions").document(tx.invoiceNumber).set(data).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing transaction ${tx.invoiceNumber} to Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun pushMutation(mutation: StockMutation) {
        coroutineScope.launch {
            try {
                val data = hashMapOf(
                    "drugId" to mutation.drugId.toString(),
                    "drugName" to mutation.drugName,
                    "batchNumber" to mutation.batchNumber,
                    "fromLocation" to mutation.fromLocation,
                    "toLocation" to mutation.toLocation,
                    "quantity" to mutation.quantity,
                    "timestamp" to mutation.timestamp,
                    "note" to mutation.note,
                    "operatorName" to mutation.operatorName
                )
                firestore.collection("stock_mutations").document(mutation.id.toString()).set(data).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing mutation to Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun pushStockOpname(record: StockOpnameRecord) {
        coroutineScope.launch {
            try {
                val data = hashMapOf(
                    "dateFormatted" to record.dateFormatted,
                    "timestamp" to record.timestamp,
                    "auditorName" to record.auditorName,
                    "totalDiscrepancyUnits" to record.totalDiscrepancyUnits,
                    "totalLossValuation" to record.totalLossValuation,
                    "status" to record.status,
                    "notes" to record.notes
                )
                firestore.collection("stock_opname_records").document(record.id.toString()).set(data).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing stock opname to Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun pushUser(user: UserAccount) {
        coroutineScope.launch {
            try {
                val data = hashMapOf(
                    "id" to user.id,
                    "username" to user.username.lowercase(),
                    "email" to user.email,
                    "fullName" to user.fullName,
                    "role" to user.role.name,
                    "phone" to user.phone
                )
                firestore.collection("user_accounts").document(user.username.lowercase()).set(data).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing user ${user.username} to Firestore: ${e.localizedMessage}")
            }
        }
    }

    /**
     * High-Speed Bulk Push for massive imports (Excel / CSV) using Firestore Batch Write.
     * Automatically splits operations into atomic 450-document chunks for max speed and reliability.
     */
    suspend fun pushBulkDrugsAndBatches(drugs: List<DrugItem>, batches: List<StockBatch>) {
        try {
            val allOps = mutableListOf<Pair<String, Map<String, Any>>>()
            drugs.forEach { drug ->
                val data = hashMapOf<String, Any>(
                    "barcode" to drug.barcode,
                    "name" to drug.name,
                    "genericName" to drug.genericName,
                    "category" to drug.category.name,
                    "indication" to drug.indication,
                    "unit" to drug.unit,
                    "buyPrice" to drug.buyPrice,
                    "sellPrice" to drug.sellPrice,
                    "minStockThreshold" to drug.minStockThreshold,
                    "isFastMoving" to drug.isFastMoving,
                    "requirePrescription" to drug.requirePrescription,
                    "isDeleted" to drug.isDeleted,
                    "lastModified" to drug.lastModified
                )
                allOps.add(Pair("drugs/${drug.id}", data))
            }
            batches.forEach { batch ->
                val data = hashMapOf<String, Any>(
                    "drugId" to batch.drugId.toString(),
                    "batchNumber" to batch.batchNumber,
                    "expiryDate" to batch.expiryDate,
                    "etalaseStock" to batch.etalaseStock,
                    "gudangStock" to batch.gudangStock,
                    "supplierName" to batch.supplierName,
                    "lastModified" to batch.lastModified
                )
                allOps.add(Pair("stock_batches/${batch.id}", data))
            }

            allOps.chunked(450).forEach { chunk ->
                val writeBatch = firestore.batch()
                chunk.forEach { (path, data) ->
                    val parts = path.split("/")
                    val docRef = firestore.collection(parts[0]).document(parts[1])
                    writeBatch.set(docRef, data)
                }
                writeBatch.commit().await()
            }
            Log.d(TAG, "Bulk push to Firestore completed: ${drugs.size} drugs, ${batches.size} batches")
        } catch (e: Exception) {
            Log.e(TAG, "Notice on bulk pushing data to Firestore: ${e.localizedMessage}")
        }
    }
}
