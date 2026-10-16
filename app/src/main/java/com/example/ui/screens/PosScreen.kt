package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CartItem
import com.example.data.model.DrugCategory
import com.example.data.model.DrugWithBatches
import com.example.data.model.StockBatch
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.FarmivaCreamBg
import com.example.ui.theme.FarmivaCreamBorder
import com.example.ui.theme.FarmivaCreamCard
import com.example.ui.theme.FarmivaCreamSecondary
import com.example.ui.theme.FarmivaForestGreen
import com.example.ui.theme.FarmivaMintSoft
import com.example.ui.theme.FarmivaPrimaryGreen
import com.example.ui.theme.FarmivaSageAccent
import com.example.ui.theme.FarmivaWhite
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FarmivaViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PosScreen(viewModel: FarmivaViewModel) {
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val discountPercent by viewModel.overallDiscountPercent.collectAsState()
    val isPrescription by viewModel.isPrescriptionSale.collectAsState()
    val doctorName by viewModel.doctorName.collectAsState()
    val prescriptionNumber by viewModel.prescriptionNumber.collectAsState()
    val profile by viewModel.pharmacyProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<DrugCategory?>(null) }

    // 0: Katalog Produk, 1: Keranjang Transaksi
    var selectedMobileTab by remember { mutableIntStateOf(0) }

    // State Pembayaran
    var selectedPaymentMethod by remember { mutableStateOf("CASH") } // "CASH" atau "QRIS"
    var cashGivenText by remember { mutableStateOf("") }
    var showQrisModal by remember { mutableStateOf(false) }

    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    val totalCartCount = cartItems.sumOf { it.quantity }
    val subtotal = cartItems.sumOf { it.lineTotal }
    val discountAmount = subtotal * (discountPercent / 100.0)
    val grandTotal = maxOf(0.0, subtotal - discountAmount)
    val cashGiven = cashGivenText.toDoubleOrNull() ?: 0.0
    val changeAmount = maxOf(0.0, cashGiven - grandTotal)

    // Filter obat berdasarkan pencarian nama, barcode, zat aktif, dan khasiat
    val filteredDrugs = remember(drugsWithBatches, searchQuery, selectedCategoryFilter) {
        val query = searchQuery.trim().lowercase()
        drugsWithBatches.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.drug.name.lowercase().contains(query) ||
                    item.drug.barcode.lowercase().contains(query) ||
                    item.drug.genericName.lowercase().contains(query) ||
                    item.drug.indication.lowercase().contains(query) ||
                    item.batches.any { it.batchNumber.lowercase().contains(query) }
            val matchesCategory = selectedCategoryFilter == null || item.drug.category == selectedCategoryFilter
            matchesQuery && matchesCategory
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FarmivaCreamBg)
            .testTag("pos_screen_root")
    ) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // ==========================================
            // TAMPILAN WIDE / TABLET: DUAL PANE BERDAMPINGAN
            // Kiri: Katalog & Pencarian Produk | Kanan: Keranjang Transaksi
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sisi Kiri: Katalog Produk & Search Bar (56% lebar)
                Card(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder)
                ) {
                    ProductCatalogPane(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedCategoryFilter = selectedCategoryFilter,
                        onCategorySelect = { selectedCategoryFilter = it },
                        filteredDrugs = filteredDrugs,
                        cartItems = cartItems,
                        formatRp = ::formatRp,
                        onAddToCart = { drug, batch -> viewModel.addToCart(drug, batch) },
                        onTestSound = { viewModel.testCashierSound() },
                        onOpenHistory = { viewModel.navigateTo(AppScreen.RIWAYAT_TRANSAKSI) }
                    )
                }

                // Sisi Kanan: Keranjang Transaksi Aktif (44% lebar)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder)
                ) {
                    TransactionCartPane(
                        cartItems = cartItems,
                        totalCartCount = totalCartCount,
                        subtotal = subtotal,
                        discountPercent = discountPercent,
                        discountAmount = discountAmount,
                        grandTotal = grandTotal,
                        cashGivenText = cashGivenText,
                        onCashGivenChange = { cashGivenText = it },
                        cashGiven = cashGiven,
                        changeAmount = changeAmount,
                        customerName = customerName,
                        onCustomerNameChange = { viewModel.setCustomerName(it) },
                        isPrescription = isPrescription,
                        doctorName = doctorName,
                        prescriptionNumber = prescriptionNumber,
                        onPrescriptionChange = { isRx, doc, rxNum ->
                            viewModel.setPrescriptionDetails(isRx, doc, rxNum)
                        },
                        selectedPaymentMethod = selectedPaymentMethod,
                        onPaymentMethodSelect = { method ->
                            selectedPaymentMethod = method
                            if (method == "QRIS") showQrisModal = true
                        },
                        formatRp = ::formatRp,
                        onUpdateQty = { drugId, batchId, qty -> viewModel.updateCartItemQuantity(drugId, batchId, qty) },
                        onRemoveItem = { drugId, batchId -> viewModel.removeCartItem(drugId, batchId) },
                        onClearCart = { viewModel.clearCart() },
                        onDiscountChange = { viewModel.setDiscountPercent(it) },
                        onProcessSale = {
                            val cashToSubmit = if (selectedPaymentMethod == "CASH") {
                                if (cashGiven >= grandTotal) cashGiven else grandTotal
                            } else grandTotal
                            viewModel.checkout(selectedPaymentMethod, cashToSubmit)
                            cashGivenText = ""
                        }
                    )
                }
            }
        } else {
            // ==========================================
            // TAMPILAN SMARTPHONE / VERTICAL HANDHELD
            // Header Tab: [Katalog Produk] & [Keranjang Transaksi (N)]
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Segmented Tab Selector
                TabRow(
                    selectedTabIndex = selectedMobileTab,
                    containerColor = FarmivaCreamSecondary,
                    contentColor = FarmivaForestGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("pos_mode_tabs"),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedMobileTab]),
                            color = FarmivaPrimaryGreen,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedMobileTab == 0,
                        onClick = { selectedMobileTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Katalog Obat", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("pos_catalog_tab")
                    )
                    Tab(
                        selected = selectedMobileTab == 1,
                        onClick = { selectedMobileTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgedBox(
                                    badge = {
                                        if (totalCartCount > 0) {
                                            Badge(
                                                containerColor = AlertExpiringRed,
                                                contentColor = Color.White
                                            ) {
                                                Text("$totalCartCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Keranjang", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("pos_cart_tab")
                    )
                    Tab(
                        selected = selectedMobileTab == 2,
                        onClick = { selectedMobileTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Riwayat", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("pos_history_tab")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedMobileTab == 0) {
                        // Tab 0: Katalog Produk & Pencarian
                        ProductCatalogPane(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedCategoryFilter = selectedCategoryFilter,
                            onCategorySelect = { selectedCategoryFilter = it },
                            filteredDrugs = filteredDrugs,
                            cartItems = cartItems,
                            formatRp = ::formatRp,
                            onAddToCart = { drug, batch -> viewModel.addToCart(drug, batch) },
                            onTestSound = { viewModel.testCashierSound() },
                            onOpenHistory = { selectedMobileTab = 2 }
                        )

                        // Floating Cart Bar (Muncul mulus bila ada obat di keranjang)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = cartItems.isNotEmpty(),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = FarmivaForestGreen,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedMobileTab = 1 }
                                    .testTag("pos_floating_cart_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "$totalCartCount Item di Keranjang",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Total: ${formatRp(grandTotal)}",
                                                color = FarmivaMintSoft,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Buka & Bayar",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (selectedMobileTab == 1) {
                        // Tab 1: Keranjang Transaksi & Kasir Checkout
                        TransactionCartPane(
                            cartItems = cartItems,
                            totalCartCount = totalCartCount,
                            subtotal = subtotal,
                            discountPercent = discountPercent,
                            discountAmount = discountAmount,
                            grandTotal = grandTotal,
                            cashGivenText = cashGivenText,
                            onCashGivenChange = { cashGivenText = it },
                            cashGiven = cashGiven,
                            changeAmount = changeAmount,
                            customerName = customerName,
                            onCustomerNameChange = { viewModel.setCustomerName(it) },
                            isPrescription = isPrescription,
                            doctorName = doctorName,
                            prescriptionNumber = prescriptionNumber,
                            onPrescriptionChange = { isRx, doc, rxNum ->
                                viewModel.setPrescriptionDetails(isRx, doc, rxNum)
                            },
                            selectedPaymentMethod = selectedPaymentMethod,
                            onPaymentMethodSelect = { method ->
                                selectedPaymentMethod = method
                                if (method == "QRIS") showQrisModal = true
                            },
                            formatRp = ::formatRp,
                            onUpdateQty = { drugId, batchId, qty -> viewModel.updateCartItemQuantity(drugId, batchId, qty) },
                            onRemoveItem = { drugId, batchId -> viewModel.removeCartItem(drugId, batchId) },
                            onClearCart = { viewModel.clearCart() },
                            onDiscountChange = { viewModel.setDiscountPercent(it) },
                            onProcessSale = {
                                val cashToSubmit = if (selectedPaymentMethod == "CASH") {
                                    if (cashGiven >= grandTotal) cashGiven else grandTotal
                                } else grandTotal
                                viewModel.checkout(selectedPaymentMethod, cashToSubmit)
                                cashGivenText = ""
                                selectedMobileTab = 0 // Kembali ke katalog setelah selesai
                            }
                        )
                    } else {
                        // Tab 2: Riwayat Transaksi Masa Lalu & Laporan Harian
                        PastTransactionsScreen(
                            viewModel = viewModel,
                            onBack = { selectedMobileTab = 0 }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // MODAL QRIS DINAMIS
    // ==========================================
    if (showQrisModal) {
        Dialog(onDismissRequest = { showQrisModal = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QRIS Dynamic Merchant Apotek",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FarmivaForestGreen
                        )
                        IconButton(onClick = { showQrisModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Text(
                        text = "Scan untuk membayar via GoPay, OVO, Dana, BCA, Livin & Mobile Banking",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .size(190.dp)
                            .border(2.dp, Color(0xFF1E88E5), RoundedCornerShape(12.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = "QRIS",
                                tint = Color.Black,
                                modifier = Modifier.size(130.dp)
                            )
                            Text("NMID: ID102026FARMIVA01", fontSize = 9.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Total Tagihan: ${formatRp(grandTotal)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E88E5)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            showQrisModal = false
                            viewModel.checkout("QRIS", grandTotal)
                            cashGivenText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Konfirmasi Pembayaran QRIS Berhasil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * PANE 1: KATALOG PRODUK & SEARCH BAR
 */
@Composable
fun ProductCatalogPane(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategoryFilter: DrugCategory?,
    onCategorySelect: (DrugCategory?) -> Unit,
    filteredDrugs: List<DrugWithBatches>,
    cartItems: List<CartItem>,
    formatRp: (Double) -> String,
    onAddToCart: (DrugWithBatches, StockBatch) -> Unit,
    onTestSound: () -> Unit,
    onOpenHistory: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // 1. Bilah Pencarian Produk & Tombol Coba Suara & Riwayat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari nama obat, barcode, zat aktif, khasiat...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FarmivaPrimaryGreen) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = FarmivaPrimaryGreen,
                    unfocusedBorderColor = FarmivaCreamBorder
                )
            )

            // Tombol Coba Suara Kasir
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FarmivaMintSoft,
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTestSound() }
                    .testTag("test_cashier_sound_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Coba Suara Kasir",
                        tint = FarmivaForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tes Suara",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmivaForestGreen
                    )
                }
            }

            if (onOpenHistory != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FarmivaCreamSecondary,
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenHistory() }
                        .testTag("open_history_from_pos_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Riwayat Transaksi",
                            tint = FarmivaForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Riwayat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FarmivaForestGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Filter Kategori Obat
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("Semua (${filteredDrugs.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmivaMintSoft,
                        selectedLabelColor = FarmivaForestGreen
                    )
                )
            }
            DrugCategory.values().forEach { cat ->
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            onCategorySelect(if (selectedCategoryFilter == cat) null else cat)
                        },
                        label = { Text(cat.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmivaMintSoft,
                            selectedLabelColor = FarmivaForestGreen
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Daftar Kartu Obat
        if (filteredDrugs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Obat tidak ditemukan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Coba kata kunci lain atau periksa filter kategori.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDrugs, key = { it.drug.id }) { drugItem ->
                    val inCartQty = cartItems.filter { it.drug.id == drugItem.drug.id }.sumOf { it.quantity }
                    PosDrugCard(
                        drugItem = drugItem,
                        inCartQty = inCartQty,
                        formatRp = formatRp,
                        onAddToCart = { batch -> onAddToCart(drugItem, batch) }
                    )
                }
            }
        }
    }
}

/**
 * PANE 2: KERANJANG TRANSAKSI & PEMPROSESAN PENJUALAN
 */
@Composable
fun TransactionCartPane(
    cartItems: List<CartItem>,
    totalCartCount: Int,
    subtotal: Double,
    discountPercent: Double,
    discountAmount: Double,
    grandTotal: Double,
    cashGivenText: String,
    onCashGivenChange: (String) -> Unit,
    cashGiven: Double,
    changeAmount: Double,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    isPrescription: Boolean,
    doctorName: String,
    prescriptionNumber: String,
    onPrescriptionChange: (Boolean, String, String) -> Unit,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    formatRp: (Double) -> String,
    onUpdateQty: (Long, Long, Int) -> Unit,
    onRemoveItem: (Long, Long) -> Unit,
    onClearCart: () -> Unit,
    onDiscountChange: (Double) -> Unit,
    onProcessSale: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Header Keranjang & Tombol Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = FarmivaForestGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Keranjang Transaksi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FarmivaForestGreen
                )
                if (totalCartCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = FarmivaMintSoft,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "$totalCartCount item",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmivaForestGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (cartItems.isNotEmpty()) {
                TextButton(
                    onClick = onClearCart,
                    modifier = Modifier.testTag("pos_clear_cart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Semua",
                        tint = AlertExpiringRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kosongkan", fontSize = 11.sp, color = AlertExpiringRed)
                }
            }
        }

        HorizontalDivider(color = FarmivaCreamBorder, modifier = Modifier.padding(vertical = 6.dp))

        if (cartItems.isEmpty()) {
            // Tampilan jika keranjang kosong
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keranjang Transaksi Kosong",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pilih dan klik tombol '+ Tambah' pada katalog obat.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Konten Keranjang & Checkout Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Daftar Item di Keranjang
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cartItems.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FarmivaCreamSecondary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.drug.name,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Batch ${item.batch.batchNumber} • Exp: ${item.batch.expiryDate}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${formatRp(item.unitPrice)} × ${item.quantity} = ${formatRp(item.lineTotal)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FarmivaPrimaryGreen
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateQty(item.drug.id, item.batch.id, item.quantity - 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${item.quantity}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { onUpdateQty(item.drug.id, item.batch.id, item.quantity + 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onRemoveItem(item.drug.id, item.batch.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FarmivaCreamBorder)

                // 2. Data Pelanggan & Resep Dokter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Data Pelanggan & Jenis Transaksi:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = onCustomerNameChange,
                        placeholder = { Text("Nama Pelanggan / Pasien", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pos_customer_name_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPrescriptionChange(!isPrescription, doctorName, prescriptionNumber) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPrescription,
                            onCheckedChange = { onPrescriptionChange(it, doctorName, prescriptionNumber) },
                            colors = CheckboxDefaults.colors(checkedColor = FarmivaPrimaryGreen)
                        )
                        Text("Transaksi Resep Dokter (Wajib SIPNAP)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (isPrescription) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = doctorName,
                                onValueChange = { onPrescriptionChange(true, it, prescriptionNumber) },
                                placeholder = { Text("Nama Dokter", fontSize = 10.5.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = prescriptionNumber,
                                onValueChange = { onPrescriptionChange(true, doctorName, it) },
                                placeholder = { Text("No. Resep", fontSize = 10.5.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = FarmivaCreamBorder)

                // 3. Diskon Transaksi & Kalkulasi
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Diskon Penjualan:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.0, 5.0, 10.0).forEach { pct ->
                                FilterChip(
                                    selected = discountPercent == pct,
                                    onClick = { onDiscountChange(pct) },
                                    label = { Text("${pct.toInt()}%", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FarmivaMintSoft,
                                        selectedLabelColor = FarmivaForestGreen
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatRp(subtotal), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (discountAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Potongan Diskon (${discountPercent.toInt()}%):", fontSize = 11.sp, color = Color(0xFFD97706))
                            Text("-${formatRp(discountAmount)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total Tagihan:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatRp(grandTotal),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmivaPrimaryGreen
                        )
                    }
                }

                HorizontalDivider(color = FarmivaCreamBorder)

                // 4. Metode Pembayaran
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Metode Pembayaran:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedPaymentMethod == "CASH",
                            onClick = { onPaymentMethodSelect("CASH") },
                            label = { Text("TUNAI (CASH)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmivaMintSoft,
                                selectedLabelColor = FarmivaForestGreen
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedPaymentMethod == "QRIS",
                            onClick = { onPaymentMethodSelect("QRIS") },
                            label = { Text("QRIS DINAMIS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE0F2FE),
                                selectedLabelColor = Color(0xFF0369A1)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedPaymentMethod == "CASH") {
                        OutlinedTextField(
                            value = cashGivenText,
                            onValueChange = onCashGivenChange,
                            placeholder = { Text("Nominal Uang Tunai Diterima (Rp)...", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pos_cash_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Tombol Cepat Uang Pas, 50k, 100k, 200k
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onCashGivenChange(grandTotal.toInt().toString()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Uang Pas", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { onCashGivenChange("50000") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("50k", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { onCashGivenChange("100000") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("100k", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { onCashGivenChange("200000") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("200k", fontSize = 10.sp)
                            }
                        }

                        // Perhitungan Kembalian
                        if (cashGiven > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (cashGiven >= grandTotal) FarmivaMintSoft else Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (cashGiven >= grandTotal) "Kembalian Pelanggan:" else "Uang Tunai Kurang:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (cashGiven >= grandTotal) FarmivaForestGreen else Color(0xFFDC2626)
                                    )
                                    Text(
                                        text = if (cashGiven >= grandTotal) formatRp(changeAmount) else "-${formatRp(grandTotal - cashGiven)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cashGiven >= grandTotal) FarmivaForestGreen else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5. Tombol Selesaikan Pembayaran & Cetak Struk
                Button(
                    onClick = onProcessSale,
                    colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pos_checkout_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Selesaikan Pembayaran & Cetak Struk", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Kartu Obat di Katalog POS
 */
@Composable
fun PosDrugCard(
    drugItem: DrugWithBatches,
    inCartQty: Int,
    formatRp: (Double) -> String,
    onAddToCart: (StockBatch) -> Unit
) {
    val drug = drugItem.drug
    val fefoBatch = drugItem.fefoBatches.firstOrNull { it.etalaseStock > 0 } ?: drugItem.fefoBatches.firstOrNull()
    val availableEtalase = drugItem.totalEtalase
    val isAvailable = availableEtalase > 0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("drug_card_${drug.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(drug.category.badgeColorHex),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = drug.category.displayName,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (drug.isFastMoving) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = FarmivaSageAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "FAST MOVING",
                                fontSize = 8.sp,
                                color = FarmivaForestGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (inCartQty > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = FarmivaMintSoft,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Di Keranjang: $inCartQty",
                                fontSize = 8.5.sp,
                                color = FarmivaForestGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = drug.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${drug.genericName} • ${drug.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatRp(drug.sellPrice),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaPrimaryGreen
                    )
                    Text(
                        text = "Etalase: $availableEtalase",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailable) MaterialTheme.colorScheme.onSurface else AlertExpiringRed
                    )
                    fefoBatch?.let { b ->
                        Text(
                            text = "FEFO Exp: ${b.expiryDate}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { fefoBatch?.let { onAddToCart(it) } },
                enabled = isAvailable && fefoBatch != null,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FarmivaPrimaryGreen
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_to_cart_btn_${drug.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Tambah", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
