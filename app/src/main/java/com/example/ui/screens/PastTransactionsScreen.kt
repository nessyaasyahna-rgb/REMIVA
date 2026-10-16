package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Transaction
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.FarmivaCreamBg
import com.example.ui.theme.FarmivaCreamBorder
import com.example.ui.theme.FarmivaCreamCard
import com.example.ui.theme.FarmivaForestGreen
import com.example.ui.theme.FarmivaMintSoft
import com.example.ui.theme.FarmivaPrimaryGreen
import com.example.ui.theme.FarmivaWhite
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FarmivaViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterPreset(val label: String) {
    TODAY("Hari Ini"),
    YESTERDAY("Kemarin"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    ALL("Semua")
}

@Composable
fun PastTransactionsScreen(
    viewModel: FarmivaViewModel,
    onBack: () -> Unit = { viewModel.navigateTo(AppScreen.POS_KASIR) }
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val profile by viewModel.pharmacyProfile.collectAsState()

    var selectedPreset by remember { mutableStateOf(DateFilterPreset.TODAY) }
    var customDateQuery by remember { mutableStateOf("") } // YYYY-MM-DD or custom text
    var searchKeyword by remember { mutableStateOf("") }
    var selectedPaymentFilter by remember { mutableStateOf<String?>(null) } // null = All, "CASH", "QRIS"
    var filterOnlyPrescription by remember { mutableStateOf(false) }

    var showDailyReportModal by remember { mutableStateOf(false) }

    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    val dateFormatDay = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullDateTimeFormatter = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID")) }

    val now = remember { Date() }
    val todayStr = remember { dateFormatDay.format(now) }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dateFormatDay.format(cal.time)
    }
    val sevenDaysAgoTime = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        cal.timeInMillis
    }
    val thisMonthPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(now) }

    // Filtered Transactions
    val filteredTransactions = remember(
        transactions,
        selectedPreset,
        customDateQuery,
        searchKeyword,
        selectedPaymentFilter,
        filterOnlyPrescription
    ) {
        transactions.filter { tx ->
            val txDateStr = dateFormatDay.format(Date(tx.timestamp))

            // 1. Date Filter
            val matchesDate = if (customDateQuery.isNotBlank()) {
                val q = customDateQuery.trim().lowercase()
                txDateStr.contains(q) || displayDateFormatter.format(Date(tx.timestamp)).lowercase().contains(q)
            } else {
                when (selectedPreset) {
                    DateFilterPreset.TODAY -> txDateStr == todayStr
                    DateFilterPreset.YESTERDAY -> txDateStr == yesterdayStr
                    DateFilterPreset.LAST_7_DAYS -> tx.timestamp >= sevenDaysAgoTime
                    DateFilterPreset.THIS_MONTH -> txDateStr.startsWith(thisMonthPrefix)
                    DateFilterPreset.ALL -> true
                }
            }

            // 2. Keyword Filter (Invoice, Customer, Cashier, Medicines in summary)
            val matchesKeyword = if (searchKeyword.isBlank()) true else {
                val q = searchKeyword.trim().lowercase()
                tx.invoiceNumber.lowercase().contains(q) ||
                        tx.customerName.lowercase().contains(q) ||
                        tx.cashierName.lowercase().contains(q) ||
                        tx.itemsSummary.lowercase().contains(q)
            }

            // 3. Payment Filter
            val matchesPayment = selectedPaymentFilter == null || tx.paymentMethod.equals(selectedPaymentFilter, ignoreCase = true)

            // 4. Prescription Filter
            val matchesPrescription = !filterOnlyPrescription || tx.isPrescription

            matchesDate && matchesKeyword && matchesPayment && matchesPrescription
        }.sortedByDescending { it.timestamp }
    }

    // Daily Report Calculations on filtered list
    val totalCount = filteredTransactions.size
    val totalRevenue = filteredTransactions.sumOf { it.grandTotal }
    val totalCash = filteredTransactions.filter { it.paymentMethod.equals("CASH", ignoreCase = true) }.sumOf { it.grandTotal }
    val totalQris = filteredTransactions.filter { it.paymentMethod.equals("QRIS", ignoreCase = true) }.sumOf { it.grandTotal }
    val totalDiscount = filteredTransactions.sumOf { it.discountAmount }
    val averageTicket = if (totalCount > 0) totalRevenue / totalCount else 0.0

    // Filter label for display
    val activeDateLabel = if (customDateQuery.isNotBlank()) {
        "Pencarian Tanggal: $customDateQuery"
    } else {
        when (selectedPreset) {
            DateFilterPreset.TODAY -> "Hari Ini (${displayDateFormatter.format(now)})"
            DateFilterPreset.YESTERDAY -> "Kemarin (${yesterdayStr})"
            DateFilterPreset.LAST_7_DAYS -> "7 Hari Terakhir"
            DateFilterPreset.THIS_MONTH -> "Bulan Ini (${SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(now)})"
            DateFilterPreset.ALL -> "Semua Riwayat"
        }
    }

    // Daily Report Text Generator
    fun generateDailyReportSummary(): String {
        return buildString {
            appendLine("==========================================")
            appendLine("      LAPORAN PENJUALAN HARIAN KASIR     ")
            appendLine("           APOTEK ${profile.pharmacyName.uppercase()}        ")
            appendLine("==========================================")
            appendLine("Periode/Tanggal : $activeDateLabel")
            appendLine("Dicetak Pada    : ${fullDateTimeFormatter.format(Date())}")
            appendLine("Petugas Kasir   : ${profile.apotekerName}")
            appendLine("------------------------------------------")
            appendLine("RINGKASAN KASIR:")
            appendLine("Total Transaksi : $totalCount Transaksi")
            appendLine("TOTAL OMZET     : ${formatRp(totalRevenue)}")
            appendLine("Tunai (Cash)    : ${formatRp(totalCash)}")
            appendLine("Non-Tunai (QRIS): ${formatRp(totalQris)}")
            appendLine("Total Diskon    : ${formatRp(totalDiscount)}")
            appendLine("Rata-rata/Struk : ${formatRp(averageTicket)}")
            appendLine("------------------------------------------")
            appendLine("DAFTAR NOTA TRANSAKSI:")
            filteredTransactions.forEachIndexed { idx, tx ->
                val timeStr = timeFormatter.format(Date(tx.timestamp))
                appendLine("${idx + 1}. #${tx.invoiceNumber} [$timeStr]")
                appendLine("   Pelanggan: ${tx.customerName} | Bayar: ${tx.paymentMethod}")
                appendLine("   Items: ${tx.itemsSummary}")
                appendLine("   Total: ${formatRp(tx.grandTotal)}")
            }
            appendLine("==========================================")
            appendLine("Simpan rekap ini untuk serah terima shift kasir.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FarmivaCreamBg)
            .testTag("past_transactions_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // ==========================================
            // HEADER BAR: BACK BUTTON & TITLE & ACTION
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FarmivaWhite)
                            .border(1.dp, FarmivaCreamBorder, CircleShape)
                            .testTag("back_to_pos_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Kasir",
                            tint = FarmivaForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Riwayat Transaksi",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmivaForestGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FarmivaMintSoft
                            ) {
                                Text(
                                    text = "$totalCount Struk",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmivaPrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Laporan harian penjualan & pencarian nota kasir",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tombol Rekap Laporan Harian
                Button(
                    onClick = { showDailyReportModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmivaForestGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("open_daily_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rekap Harian", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // KARTU RINGKASAN OMZET HARIAN (DAILY REPORTING)
            // ==========================================
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_report_summary_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = FarmivaPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeDateLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmivaForestGreen
                            )
                        }

                        Text(
                            text = "${filteredTransactions.size} Transaksi",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Omzet / Grand Total
                        Surface(
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            color = FarmivaMintSoft
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "TOTAL OMZET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FarmivaForestGreen
                                )
                                Text(
                                    text = formatRp(totalRevenue),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FarmivaForestGreen
                                )
                            }
                        }

                        // Tunai (Cash in Register)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "TUNAI (CASH)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = formatRp(totalCash),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        // Non-Tunai / QRIS
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "QRIS / DIGITAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    text = formatRp(totalQris),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // KONTROL PENCARIAN & FILTER TANGGAL
            // ==========================================
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search Bar (Keyword & Tanggal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchKeyword,
                            onValueChange = { searchKeyword = it },
                            placeholder = { Text("Cari No. Struk, Pelanggan, atau Nama Obat...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = FarmivaPrimaryGreen)
                            },
                            trailingIcon = {
                                if (searchKeyword.isNotEmpty()) {
                                    IconButton(onClick = { searchKeyword = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Color.Gray)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = FarmivaPrimaryGreen,
                                unfocusedBorderColor = FarmivaCreamBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transaction_search_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Custom Date input
                        OutlinedTextField(
                            value = customDateQuery,
                            onValueChange = { customDateQuery = it },
                            placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (customDateQuery.isNotEmpty()) {
                                    IconButton(onClick = { customDateQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = FarmivaPrimaryGreen,
                                unfocusedBorderColor = FarmivaCreamBorder
                            ),
                            modifier = Modifier
                                .width(140.dp)
                                .testTag("transaction_date_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Chips Bar
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(DateFilterPreset.values()) { preset ->
                            val isSelected = selectedPreset == preset && customDateQuery.isEmpty()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    customDateQuery = ""
                                    selectedPreset = preset
                                },
                                label = { Text(preset.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FarmivaPrimaryGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.width(4.dp))
                            // Filter Tunai
                            FilterChip(
                                selected = selectedPaymentFilter == "CASH",
                                onClick = {
                                    selectedPaymentFilter = if (selectedPaymentFilter == "CASH") null else "CASH"
                                },
                                label = { Text("Tunai", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        item {
                            // Filter QRIS
                            FilterChip(
                                selected = selectedPaymentFilter == "QRIS",
                                onClick = {
                                    selectedPaymentFilter = if (selectedPaymentFilter == "QRIS") null else "QRIS"
                                },
                                label = { Text("QRIS", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        item {
                            // Filter Resep Dokter
                            FilterChip(
                                selected = filterOnlyPrescription,
                                onClick = { filterOnlyPrescription = !filterOnlyPrescription },
                                label = { Text("Resep Dokter", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // DAFTAR TRANSAKSI MASA LALU (PAST TRANSACTIONS)
            // ==========================================
            if (filteredTransactions.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = FarmivaWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak Ada Transaksi Ditemukan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FarmivaForestGreen
                        )
                        Text(
                            text = "Tidak ada nota penjualan pada filter '$activeDateLabel' atau kata kunci ini.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                searchKeyword = ""
                                customDateQuery = ""
                                selectedPreset = DateFilterPreset.ALL
                                selectedPaymentFilter = null
                                filterOnlyPrescription = false
                            }
                        ) {
                            Text("Tampilkan Semua Riwayat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("past_transactions_list"),
                    contentPadding = PaddingValues(bottom = 90.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        PastTransactionItemCard(
                            transaction = tx,
                            formatRp = ::formatRp,
                            onViewReceipt = { viewModel.openReceiptModal(tx) },
                            onPlayVoice = { viewModel.speakPaymentReceipt(tx.grandTotal) }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG REKAP LAPORAN HARIAN / TUTUP KASIR
    // ==========================================
    if (showDailyReportModal) {
        val reportSummary = remember(filteredTransactions, activeDateLabel) {
            generateDailyReportSummary()
        }

        Dialog(onDismissRequest = { showDailyReportModal = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("daily_report_modal")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Rekap Laporan Harian",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FarmivaForestGreen
                            )
                            Text(
                                text = activeDateLabel,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showDailyReportModal = false }) {
                            Icon(Icons.Default.Clear, contentDescription = "Tutup")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = FarmivaCreamBorder)

                    // Textbox preview struk laporan harian
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = reportSummary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF1E293B),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Rekap Laporan Farmiva", reportSummary)
                                clipboard.setPrimaryClip(clip)
                                viewModel.showToast("Rekap laporan harian berhasil disalin ke clipboard!")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Teks", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.showToast("Mencetak rekap penutupan kasir ke printer thermal...")
                                showDailyReportModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmivaForestGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak Kasir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kartu baris transaksi lampau yang rapi dan informatif
 */
@Composable
fun PastTransactionItemCard(
    transaction: Transaction,
    formatRp: (Double) -> String,
    onViewReceipt: () -> Unit,
    onPlayVoice: () -> Unit
) {
    val fullDateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID")).format(Date(transaction.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewReceipt() }
            .testTag("past_transaction_card_${transaction.invoiceNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Baris: Invoice & Waktu + Badge Pembayaran
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${transaction.invoiceNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FarmivaForestGreen
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Badge Metode Pembayaran
                    val isCash = transaction.paymentMethod.equals("CASH", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCash) FarmivaMintSoft else Color(0xFFEFF6FF)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isCash) Icons.Default.Payments else Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = if (isCash) FarmivaForestGreen else Color(0xFF1D4ED8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCash) "TUNAI" else "QRIS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCash) FarmivaForestGreen else Color(0xFF1D4ED8)
                            )
                        }
                    }

                    if (transaction.isPrescription) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "RESEP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Nominal Total
                Text(
                    text = formatRp(transaction.grandTotal),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = FarmivaPrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Waktu & Kasir & Pelanggan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = fullDateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pelanggan: ${transaction.customerName}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Ringkasan Obat yang dibeli
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC)
            ) {
                Text(
                    text = transaction.itemsSummary,
                    fontSize = 11.sp,
                    color = Color(0xFF334155),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Rincian Pembayaran Kasir (Uang Diterima & Kembalian)
            if (transaction.cashGiven > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Diterima: ${formatRp(transaction.cashGiven)} • Kembali: ${formatRp(transaction.changeAmount)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Kasir: ${transaction.cashierName}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons: Lihat Struk & Putar Suara
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPlayVoice,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = FarmivaPrimaryGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dengar Suara", fontSize = 11.sp, color = FarmivaPrimaryGreen)
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onViewReceipt,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaPrimaryGreen)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp), tint = FarmivaPrimaryGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lihat Struk", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmivaPrimaryGreen)
                }
            }
        }
    }
}
