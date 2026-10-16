package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.ExportPreviewDialog
import com.example.ui.components.PharmacySalesAndTurnoverDashboard
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FarmivaViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinancialReportScreen(viewModel: FarmivaViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val profile by viewModel.pharmacyProfile.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Buku Kas Pemasukan & Pengeluaran, 1: Jurnal Transaksi POS
    var showAddExpenseModal by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }

    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    val totalRevenue = transactions.sumOf { it.grandTotal }
    val totalExpense = expenses.sumOf { it.amount }
    val netProfit = totalRevenue - totalExpense

    val financialCsvText = remember(transactions, expenses, profile) {
        buildString {
            appendLine("LAPORAN KEUANGAN & ARUS KAS APOTEK")
            appendLine("Nama Apotek: ${profile.pharmacyName}")
            appendLine("Alamat: ${profile.address}")
            appendLine("Apoteker (APA): ${profile.apotekerName}")
            appendLine("---------------------------------------------------------")
            appendLine("RINGKASAN:")
            appendLine("Total Omzet Penjualan (Pemasukan) : ${formatRp(totalRevenue)}")
            appendLine("Total Beban & Pengeluaran        : ${formatRp(totalExpense)}")
            appendLine("Laba Bersih Apotek               : ${formatRp(netProfit)}")
            appendLine("---------------------------------------------------------")
            appendLine("Tanggal,Tipe,Keterangan,Nominal,Metode/Petugas")
            transactions.forEach { tx ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                appendLine("$dateStr,PEMASUKAN,Penjualan POS #${tx.invoiceNumber},${tx.grandTotal},${tx.paymentMethod}")
            }
            expenses.forEach { ex ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ex.timestamp))
                appendLine("$dateStr,PENGELUARAN,${ex.category} - ${ex.description},-${ex.amount},${ex.recordedBy}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Financial Overview Cards (Pemasukan, Pengeluaran, Laba)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = HijauApotekPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Omzet Masuk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatRp(totalRevenue), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HijauApotekPrimary)
                }
            }

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = AlertExpiringRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pengeluaran", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatRp(totalExpense), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AlertExpiringRed)
                }
            }

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = EmeraldGreenAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Laba Bersih", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatRp(netProfit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldGreenAccent)
                }
            }
        }

        // Action Buttons: + Catat Pengeluaran & Export Resmi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddExpenseModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AlertExpiringRed),
                modifier = Modifier.weight(1f).testTag("add_expense_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Catat Pengeluaran", fontSize = 11.sp)
            }

            Button(
                onClick = { showExportModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                modifier = Modifier.weight(1f).testTag("export_finance_button")
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("KOP & Laporan PDF/XLS", fontSize = 11.sp)
            }
        }

        // Tab selection: Arus Kas Gabungan vs Transaksi POS vs Recharts Analitik
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Pengeluaran (${expenses.size})", fontSize = 10.sp) }
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                label = { Text("Penjualan POS (${transactions.size})", fontSize = 10.sp) }
            )
            FilterChip(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                label = { Text("Recharts & Turnover", fontSize = 10.sp) }
            )
        }

        if (activeTab == 0) {
            // Expenses List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (expenses.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada pengeluaran operasional yang dicatat.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(expenses) { ex ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = ex.description, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Kategori: ${ex.category} • Dicatat oleh: ${ex.recordedBy}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ex.timestamp)),
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "-${formatRp(ex.amount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertExpiringRed
                            )
                        }
                    }
                }
            }
        } else if (activeTab == 1) {
            // Transactions List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jurnal Penjualan Kasir (${transactions.size} Nota)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.RIWAYAT_TRANSAKSI) },
                            colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cari Tanggal & Rekap Harian", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(transactions) { tx ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openReceiptModal(tx) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = tx.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "+${formatRp(tx.grandTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = HijauApotekPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pelanggan: ${tx.customerName} • Kasir: ${tx.cashierName} • Metode: ${tx.paymentMethod}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = tx.itemsSummary,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            // Recharts & Turnover Analytics Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PharmacySalesAndTurnoverDashboard(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
            }
        }
    }

    // Modal: Catat Pengeluaran
    if (showAddExpenseModal) {
        var expenseCategory by remember { mutableStateOf("Operasional") }
        var expenseDescription by remember { mutableStateOf("") }
        var expenseAmountText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddExpenseModal = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Catat Pengeluaran Apotek", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showAddExpenseModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Text("Pilih Kategori:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Operasional", "Pembelian PBF", "Gaji", "Listrik & Air").forEach { cat ->
                            FilterChip(
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                                label = { Text(cat, fontSize = 9.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = expenseDescription,
                        onValueChange = { expenseDescription = it },
                        label = { Text("Deskripsi Pengeluaran") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = expenseAmountText,
                        onValueChange = { expenseAmountText = it },
                        label = { Text("Nominal (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amount = expenseAmountText.toDoubleOrNull() ?: 0.0
                            if (expenseDescription.isNotBlank() && amount > 0) {
                                viewModel.recordExpense(expenseCategory, expenseDescription, amount)
                                showAddExpenseModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertExpiringRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Pengeluaran", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal: Export Laporan Keuangan dengan KOP & TTD
    if (showExportModal) {
        ExportPreviewDialog(
            reportTitle = "Laporan Laba Rugi & Arus Kas Apotek",
            reportSubtitle = "Rekapitulasi Keuangan Komersial & Operasional Farmasi",
            csvRawData = financialCsvText,
            profile = profile,
            onDismiss = { showExportModal = false }
        )
    }
}
