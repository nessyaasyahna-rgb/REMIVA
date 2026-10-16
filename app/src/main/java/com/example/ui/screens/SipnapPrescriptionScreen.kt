package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrugCategory
import com.example.ui.components.ExportPreviewDialog
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.FarmivaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SipnapPrescriptionScreen(viewModel: FarmivaViewModel) {
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()
    val prescriptionTransactions by viewModel.prescriptionTransactions.collectAsState()
    val profile by viewModel.pharmacyProfile.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: SIPNAP Narkotika & Psikotropika, 1: Buku Register Resep
    var showExportModal by remember { mutableStateOf(false) }

    // Controlled substances for SIPNAP
    val sipnapDrugs = remember(drugsWithBatches) {
        drugsWithBatches.filter {
            it.drug.category == DrugCategory.NARKOTIKA ||
                    it.drug.category == DrugCategory.PSIKOTROPIKA ||
                    it.drug.category == DrugCategory.KERAS
        }
    }

    // Generate CSV raw text for SIPNAP report
    val sipnapCsvText = remember(sipnapDrugs, profile) {
        buildString {
            appendLine("LAPORAN SIPNAP & E-REPORT KEMENKES REPUBLIK INDONESIA")
            appendLine("Apotek: ${profile.pharmacyName} | SIA: ${profile.siaNumber}")
            appendLine("APA: ${profile.apotekerName} | SIPA: ${profile.sipaNumber}")
            appendLine("Periode: Oktober 2026")
            appendLine("---------------------------------------------------------")
            appendLine("No,Nama Obat,Kategori,Satuan,No Batch,Stok Awal,Penerimaan(PBF),Pengeluaran(Resep),Stok Akhir")
            sipnapDrugs.forEachIndexed { i, item ->
                val primaryBatch = item.batches.firstOrNull()
                val batchNo = primaryBatch?.batchNumber ?: "-"
                val stokAkhir = item.totalStock
                val stokAwal = stokAkhir + 5 // Simulative
                val pengeluaran = 5
                val penerimaan = 0
                appendLine("${i + 1},${item.drug.name},${item.drug.category.displayName},${item.drug.unit},$batchNo,$stokAwal,$penerimaan,$pengeluaran,$stokAkhir")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Laporan SIPNAP Kemenkes", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                label = { Text("Buku Register Resep (${prescriptionTransactions.size})", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        if (activeTab == 0) {
            // SIPNAP Header & Official Export Button
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sistem Pelaporan Narkotika & Psikotropika",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Standar Kemenkes RI & Dinkes (SIPNAP Compliance)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showExportModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                            modifier = Modifier.testTag("export_sipnap_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("KOP & TTD Laporan", fontSize = 11.sp)
                        }
                    }
                }
            }

            // SIPNAP Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sipnapDrugs) { item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.drug.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = Color(item.drug.category.badgeColorHex).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.drug.category.displayName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(item.drug.category.badgeColorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Grid of SIPNAP columns: Stok Awal, Masuk, Keluar, Stok Akhir
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Batch / Exp", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(item.fefoBatches.firstOrNull()?.batchNumber ?: "-", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Stok Awal", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${item.totalStock + 5}", fontSize = 10.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Resep (Keluar)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("5 unit", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Stok Akhir", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${item.totalStock} ${item.drug.unit}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HijauApotekPrimary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // BUKU REGISTER RESEP DOKTER
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (prescriptionTransactions.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada transaksi resep dokter yang dicatat.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(prescriptionTransactions) { rx ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "No. Resep: ${rx.prescriptionNumber.ifBlank { rx.invoiceNumber }}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(rx.timestamp)),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dokter Penulis: ${rx.doctorName.ifBlank { "dr. Spesialis / Umum" }}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pasien: ${rx.customerName} • Kasir/TTK: ${rx.cashierName}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Obat Resep: ${rx.itemsSummary}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Official Report Dialog with KOP, Signatures & Stamp
    if (showExportModal) {
        ExportPreviewDialog(
            reportTitle = "Laporan Resmi SIPNAP Narkotika & Psikotropika",
            reportSubtitle = "Sesuai Peraturan Menteri Kesehatan RI Tentang Pelaporan Obat Golongan Khusus",
            csvRawData = sipnapCsvText,
            profile = profile,
            onDismiss = { showExportModal = false }
        )
    }
}
