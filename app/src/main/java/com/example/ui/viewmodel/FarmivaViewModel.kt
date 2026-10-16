package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FarmivaDatabase
import com.example.data.model.CartItem
import com.example.data.model.DrugCategory
import com.example.data.model.DrugItem
import com.example.data.model.DrugWithBatches
import com.example.data.model.ExpenseItem
import com.example.data.model.OpnameDiscrepancyItem
import com.example.data.model.PharmacyProfile
import com.example.data.model.StockBatch
import com.example.data.model.StockMutation
import com.example.data.model.StockOpnameRecord
import com.example.data.model.Transaction
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.data.repository.FarmivaRepository
import com.example.data.sync.FirestoreSyncManager
import com.example.ui.sound.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen(val title: String) {
    DASHBOARD("Dashboard"),
    POS_KASIR("Kasir (POS)"),
    STOK_FEFO("Stok & FEFO"),
    GUDANG_MUTASI("Gudang & Mutasi"),
    STOCK_OPNAME("Stock Opname"),
    SIPNAP_RESEP("Resep & SIPNAP"),
    KEUANGAN_LAPORAN("Keuangan & Laporan"),
    PENGATURAN_BACKUP("Pengaturan & Backup"),
    RIWAYAT_TRANSAKSI("Riwayat Transaksi")
}

class FarmivaViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FarmivaDatabase.getDatabase(application)
    val syncManager = FirestoreSyncManager(application, db)
    val repository = FarmivaRepository(db, syncManager)
    val soundManager = SoundManager(application)

    init {
        syncManager.startRealtimeSync()
    }

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Dark Mode Switcher
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enable: Boolean? = null) {
        _isDarkMode.value = enable ?: !_isDarkMode.value
    }

    // Device Login Persistence (Hanya perlu login 1x di awal di setiap device)
    private val authPrefs = application.getSharedPreferences("farmiva_device_auth_pref", android.content.Context.MODE_PRIVATE)

    private val _isLoggedInDevice = MutableStateFlow(authPrefs.getBoolean("is_logged_in_device", false))
    val isLoggedInDevice: StateFlow<Boolean> = _isLoggedInDevice.asStateFlow()

    // Active User / Auth
    private val _currentUser = MutableStateFlow(
        if (authPrefs.getBoolean("is_logged_in_device", false)) {
            val uName = authPrefs.getString("user_username", "apoteker") ?: "apoteker"
            val fName = authPrefs.getString("user_fullname", "apt. Sarah Olivia, S.Farm") ?: "apt. Sarah Olivia, S.Farm"
            val email = authPrefs.getString("user_email", "sarah.apa@farmiva.id") ?: "sarah.apa@farmiva.id"
            val roleStr = authPrefs.getString("user_role", UserRole.APOTEKER.name) ?: UserRole.APOTEKER.name
            val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.APOTEKER }
            UserAccount(username = uName, email = email, fullName = fName, role = role)
        } else {
            UserAccount(
                username = "guest",
                email = "",
                fullName = "Tamu / Belum Login",
                role = UserRole.KASIR
            )
        }
    )
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    fun loginDevice(user: UserAccount, rememberDevice: Boolean = true) {
        _currentUser.value = user
        _isLoggedInDevice.value = true
        if (rememberDevice) {
            authPrefs.edit()
                .putBoolean("is_logged_in_device", true)
                .putString("user_username", user.username)
                .putString("user_fullname", user.fullName)
                .putString("user_email", user.email)
                .putString("user_role", user.role.name)
                .apply()
        }
        showToast("Selamat datang, ${user.fullName}! Login berhasil di perangkat ini.")
    }

    fun registerNewAccount(fullName: String, role: UserRole, username: String, onComplete: () -> Unit) {
        val cleanUsername = username.trim().lowercase()
        val user = UserAccount(
            username = cleanUsername,
            email = "$cleanUsername@farmiva.id",
            fullName = fullName.trim(),
            role = role
        )
        viewModelScope.launch {
            try {
                repository.insertUser(user)
            } catch (_: Exception) {}
            loginDevice(user, rememberDevice = true)
            onComplete()
        }
    }

    fun switchUser(user: UserAccount) {
        _currentUser.value = user
        authPrefs.edit()
            .putBoolean("is_logged_in_device", true)
            .putString("user_username", user.username)
            .putString("user_fullname", user.fullName)
            .putString("user_email", user.email)
            .putString("user_role", user.role.name)
            .apply()
        _isLoggedInDevice.value = true
    }

    fun logout() {
        val previousName = _currentUser.value.fullName
        authPrefs.edit().putBoolean("is_logged_in_device", false).apply()
        _currentUser.value = UserAccount(
            username = "guest",
            email = "",
            fullName = "Tamu / Belum Login",
            role = UserRole.KASIR
        )
        _isLoggedInDevice.value = false
        showToast("Berhasil keluar dari akun $previousName")
    }

    val isLoggedIn: Boolean get() = _currentUser.value.username != "guest"

    // Pharmacy Profile
    private val _pharmacyProfile = MutableStateFlow(PharmacyProfile())
    val pharmacyProfile: StateFlow<PharmacyProfile> = _pharmacyProfile.asStateFlow()

    fun updateProfile(newProfile: PharmacyProfile) {
        _pharmacyProfile.value = newProfile
    }

    // Cloud DB Synchronization State (Multi-Device Real-time Sync)
    val syncStatusText: StateFlow<String> = syncManager.syncStatusText
    val isCloudSynced: StateFlow<Boolean> = syncManager.isCloudSynced

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncManager.startRealtimeSync()
            repository.processOfflineQueueSync()
            delay(600)
            _isSyncing.value = false
            showToast("Sinkronisasi Realtime Cloud: Data semua perangkat telah seragam!")
        }
    }

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Data Streams
    val drugsWithBatches: StateFlow<List<DrugWithBatches>> = repository.drugsWithBatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prescriptionTransactions: StateFlow<List<Transaction>> = repository.prescriptionTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mutations: StateFlow<List<StockMutation>> = repository.allMutationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val opnames: StateFlow<List<StockOpnameRecord>> = repository.allOpnamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseItem>> = repository.allExpensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserAccount>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _customerName = MutableStateFlow("Pelanggan Umum")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _overallDiscountPercent = MutableStateFlow(0.0)
    val overallDiscountPercent: StateFlow<Double> = _overallDiscountPercent.asStateFlow()

    private val _isPrescriptionSale = MutableStateFlow(false)
    val isPrescriptionSale: StateFlow<Boolean> = _isPrescriptionSale.asStateFlow()

    private val _doctorName = MutableStateFlow("")
    val doctorName: StateFlow<String> = _doctorName.asStateFlow()

    private val _prescriptionNumber = MutableStateFlow("")
    val prescriptionNumber: StateFlow<String> = _prescriptionNumber.asStateFlow()

    // Last completed transaction for receipt preview
    private val _lastCompletedTransaction = MutableStateFlow<Transaction?>(null)
    val lastCompletedTransaction: StateFlow<Transaction?> = _lastCompletedTransaction.asStateFlow()

    private val _showReceiptModal = MutableStateFlow(false)
    val showReceiptModal: StateFlow<Boolean> = _showReceiptModal.asStateFlow()

    // Notification message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(3000)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }

        // Automatic periodic background cloud sync loop
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                runCatching {
                    syncManager.startRealtimeSync()
                    repository.processOfflineQueueSync()
                }
            }
        }
    }

    // Fitur Coba Suara Bukti Pembayaran (Kalimat nominal yang diterima)
    fun testCashierSound() {
        soundManager.speakPaymentSentence(75000.0, true)
        showToast("🔊 Memutar suara bukti pembayaran: tujuh puluh lima ribu rupiah")
    }

    fun speakPaymentReceipt(amount: Double) {
        soundManager.speakPaymentSentence(amount, true)
        showToast("🔊 Memutar ulang suara bukti pembayaran...")
    }

    suspend fun getWarehouseAndStockBackupCsv(): String {
        return repository.exportWarehouseAndStockCsv()
    }

    // POS Cart Operations
    fun addToCart(drugWithBatches: DrugWithBatches, preferredBatch: StockBatch? = null) {
        // Apply FEFO automatically: choose batch with nearest expiration that has shelf stock
        val availableBatches = drugWithBatches.fefoBatches.filter { it.etalaseStock > 0 }
        if (availableBatches.isEmpty()) {
            showToast("Stok etalase habis! Silakan lakukan mutasi dari gudang.")
            soundManager.playAlertWarning(_pharmacyProfile.value.stockAlertEnabled)
            return
        }

        val targetBatch = preferredBatch ?: availableBatches.first()
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.drug.id == drugWithBatches.drug.id && it.batch.id == targetBatch.id }

        if (existingIndex >= 0) {
            val item = currentList[existingIndex]
            if (item.quantity + 1 > targetBatch.etalaseStock) {
                showToast("Maksimal stok etalase batch ${targetBatch.batchNumber} adalah ${targetBatch.etalaseStock}")
                return
            }
            currentList[existingIndex] = item.copy(quantity = item.quantity + 1)
        } else {
            currentList.add(CartItem(drug = drugWithBatches.drug, batch = targetBatch, quantity = 1))
        }
        _cartItems.value = currentList
    }

    fun updateCartItemQuantity(drugId: Long, batchId: Long, newQty: Int) {
        if (newQty <= 0) {
            removeCartItem(drugId, batchId)
            return
        }
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.drug.id == drugId && it.batch.id == batchId }
        if (index >= 0) {
            val item = currentList[index]
            if (newQty > item.batch.etalaseStock) {
                showToast("Stok etalase batch hanya ${item.batch.etalaseStock}")
                return
            }
            currentList[index] = item.copy(quantity = newQty)
            _cartItems.value = currentList
        }
    }

    fun removeCartItem(drugId: Long, batchId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.drug.id == drugId && it.batch.id == batchId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _overallDiscountPercent.value = 0.0
        _isPrescriptionSale.value = false
        _doctorName.value = ""
        _prescriptionNumber.value = ""
    }

    fun setCustomerName(name: String) { _customerName.value = name }
    fun setDiscountPercent(disc: Double) { _overallDiscountPercent.value = disc }
    fun setPrescriptionDetails(isRx: Boolean, doc: String, rxNo: String) {
        _isPrescriptionSale.value = isRx
        _doctorName.value = doc
        _prescriptionNumber.value = rxNo
    }

    // Checkout / Selesaikan Transaksi
    fun checkout(paymentMethod: String, cashGiven: Double) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val subtotal = items.sumOf { it.lineTotal }
        val discount = subtotal * (_overallDiscountPercent.value / 100.0)
        val grandTotal = subtotal - discount
        val change = if (paymentMethod == "CASH") maxOf(0.0, cashGiven - grandTotal) else 0.0

        viewModelScope.launch {
            // Deduct stock for each batch
            for (cartItem in items) {
                repository.deductEtalaseStock(cartItem.batch.id, cartItem.quantity)
            }

            val itemsSummary = items.joinToString(", ") { "${it.drug.name} (x${it.quantity})" }
            val invoiceNo = "INV-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${(100..999).random()}"

            val transaction = Transaction(
                invoiceNumber = invoiceNo,
                timestamp = System.currentTimeMillis(),
                cashierName = _currentUser.value.fullName,
                customerName = _customerName.value.ifBlank { "Pelanggan Umum" },
                itemsSummary = itemsSummary,
                subtotal = subtotal,
                discountPercent = _overallDiscountPercent.value,
                discountAmount = discount,
                grandTotal = grandTotal,
                paymentMethod = paymentMethod,
                cashGiven = if (paymentMethod == "CASH") cashGiven else grandTotal,
                changeAmount = change,
                isPrescription = _isPrescriptionSale.value,
                doctorName = _doctorName.value,
                prescriptionNumber = _prescriptionNumber.value
            )

            repository.recordTransaction(transaction)

            // Suara bukti pembayaran yang mengeluarkan kalimat harga yang diterima
            soundManager.speakPaymentSentence(grandTotal, _pharmacyProfile.value.cashierAudioEnabled)

            // Open receipt modal
            _lastCompletedTransaction.value = transaction
            _showReceiptModal.value = true

            // Reset cart
            clearCart()
            showToast("Transaksi Berhasil! Struk siap dicetak.")
        }
    }

    fun dismissReceiptModal() {
        _showReceiptModal.value = false
    }

    fun openReceiptModal(transaction: Transaction) {
        _lastCompletedTransaction.value = transaction
        _showReceiptModal.value = true
    }

    // Warehouse Mutation
    fun mutateStock(drugId: Long, drugName: String, batchId: Long, batchNumber: String, qty: Int, note: String) {
        viewModelScope.launch {
            repository.mutateWarehouseToShelf(
                drugId = drugId,
                drugName = drugName,
                batchId = batchId,
                batchNumber = batchNumber,
                qty = qty,
                operatorName = _currentUser.value.fullName,
                note = note
            )
            showToast("Berhasil memindahkan $qty unit $drugName ke Etalase!")
        }
    }

    // Stock Opname Commit
    fun commitStockOpname(reconciliationList: List<OpnameDiscrepancyItem>, notes: String) {
        viewModelScope.launch {
            val totalDiscrepancyUnits = reconciliationList.sumOf { it.diffUnits }
            val totalLossValuation = reconciliationList.sumOf { it.lossValue }
            val record = StockOpnameRecord(
                dateFormatted = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date()),
                timestamp = System.currentTimeMillis(),
                auditorName = _currentUser.value.fullName,
                totalDiscrepancyUnits = totalDiscrepancyUnits,
                totalLossValuation = totalLossValuation,
                status = "COMPLETED",
                notes = notes.ifBlank { "Rekonsiliasi Stok Etalase & Gudang Berhasil Disimpan" }
            )
            repository.recordOpname(record)

            // Update batches to physical counts
            for (item in reconciliationList) {
                repository.updateBatch(
                    item.batch.copy(
                        etalaseStock = item.physicalEtalase,
                        gudangStock = item.physicalGudang
                    )
                )
            }
            showToast("Stock Opname Berhasil Disimpan! Nilai Selisih Terhitung.")
        }
    }

    // Add New Drug & Batch
    fun addNewDrugWithBatch(
        name: String,
        barcode: String,
        genericName: String,
        category: DrugCategory,
        indication: String,
        unit: String,
        buyPrice: Double,
        sellPrice: Double,
        minStock: Int,
        isFastMoving: Boolean,
        requirePrescription: Boolean,
        batchNumber: String,
        expiryDate: String,
        etalaseStock: Int,
        gudangStock: Int
    ) {
        viewModelScope.launch {
            val drugId = repository.insertDrug(
                DrugItem(
                    barcode = barcode.ifBlank { "8999${System.currentTimeMillis() % 100000}" },
                    name = name,
                    genericName = genericName.ifBlank { name },
                    category = category,
                    indication = indication.ifBlank { "Sesuai resep/indikasi umum" },
                    unit = unit.ifBlank { "Strip" },
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    minStockThreshold = minStock,
                    isFastMoving = isFastMoving,
                    requirePrescription = requirePrescription
                )
            )
            repository.insertBatch(
                StockBatch(
                    drugId = drugId,
                    batchNumber = batchNumber.ifBlank { "B-FARMIVA01" },
                    expiryDate = expiryDate.ifBlank { "2027-12-31" },
                    etalaseStock = etalaseStock,
                    gudangStock = gudangStock
                )
            )
            showToast("Obat $name berhasil ditambahkan ke sistem FEFO!")
        }
    }

    // Record Expense
    fun recordExpense(category: String, description: String, amount: Double) {
        viewModelScope.launch {
            repository.recordExpense(
                ExpenseItem(
                    timestamp = System.currentTimeMillis(),
                    category = category,
                    description = description,
                    amount = amount,
                    recordedBy = _currentUser.value.fullName
                )
            )
            showToast("Pengeluaran operasional Rp $amount berhasil dicatat.")
        }
    }

    // Bulk Import Excel / CSV
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun importCsvData(csvText: String, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val count = repository.parseAndImportCsvData(csvText)
                showToast("Bulk Import Selesai: $count item obat berhasil diimpor & disinkronkan!")
                onComplete?.invoke(count)
            } catch (e: Exception) {
                showToast("Kendala impor: ${e.localizedMessage}")
            } finally {
                _isImporting.value = false
            }
        }
    }

    // Play cashier notification sound for test button
    fun playCashierChime() {
        soundManager.playCashierSuccess(true)
    }

    // Delete Drug from Stock (handles duplicate, wrong input, discontinued/unavailable)
    fun deleteDrug(drug: DrugItem, reason: String = "Data ganda / Salah input") {
        viewModelScope.launch {
            repository.deleteDrug(drug, reason)
            showToast("Obat ${drug.name} berhasil dihapus dari stok ($reason).")
        }
    }

    // Delete single Batch from Stock
    fun deleteBatch(batch: StockBatch, drugName: String = "", reason: String = "Data ganda / Salah input") {
        viewModelScope.launch {
            repository.deleteBatch(batch, drugName, reason)
            val label = if (drugName.isNotBlank()) "Batch ${batch.batchNumber} ($drugName)" else "Batch ${batch.batchNumber}"
            showToast("$label berhasil dihapus ($reason).")
        }
    }
}
