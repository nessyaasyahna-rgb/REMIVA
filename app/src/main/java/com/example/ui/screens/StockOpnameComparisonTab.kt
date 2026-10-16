package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OpnameDiscrepancyItem
import com.example.data.model.PharmacyProfile
import com.example.data.model.UserAccount
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.AlertWarningAmber
import com.example.ui.theme.FarmivaCreamBg
import com.example.ui.theme.FarmivaCreamBorder
import com.example.ui.theme.FarmivaCreamCard
import com.example.ui.theme.FarmivaCreamSecondary
import com.example.ui.theme.FarmivaForestGreen
import com.example.ui.theme.FarmivaLightMint
import com.example.ui.theme.FarmivaMintSoft
import com.example.ui.theme.FarmivaPrimaryGreen
import com.example.ui.theme.FarmivaWhite
import com.example.util.DinasReportHelper
import com.example.util.ExcelHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StockOpnameComparisonTab(
    reconciliationList: List<OpnameDiscrepancyItem>,
    pharmacyProfile: PharmacyProfile,
    currentUser: UserAccount,
    opnameNotes: String,
    onEditManualItem: (OpnameDiscrepancyItem) -> Unit,
    onResetItemToSystem: (OpnameDiscrepancyItem) -> Unit,
    onCommitAudit: () -> Unit,
    onExportXmlString: (List<OpnameDiscrepancyItem>) -> String,
    showToast: (String) -> Unit
) {
    val context = LocalContext.current
    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    var comparisonSearchQuery by remember { mutableStateOf("") }
    var comparisonFilterMode by remember { mutableStateOf(0) } // 0: Yang Berubah Saja, 1: Penyesuaian Manual, 2: Semua Item

    val totalSKUs = reconciliationList.size
    val totalSystemValuation = reconciliationList.sumOf { it.systemValuation }
    val totalPhysicalValuation = reconciliationList.sumOf { it.physicalValuation }
    val totalNetValuationDiff = totalPhysicalValuation - totalSystemValuation
    val itemsWithChange = reconciliationList.filter { it.diffUnits != 0 || it.isManuallyAdjusted }
    val manuallyAdjustedItems = reconciliationList.filter { it.isManuallyAdjusted }
    val totalLossValue = reconciliationList.sumOf { it.lossValue }

    val filteredList = remember(reconciliationList, comparisonSearchQuery, comparisonFilterMode) {
        reconciliationList.filter { item ->
            val matchSearch = comparisonSearchQuery.isBlank() ||
                    item.drug.name.contains(comparisonSearchQuery, ignoreCase = true) ||
                    item.batch.batchNumber.contains(comparisonSearchQuery, ignoreCase = true) ||
                    item.drug.barcode.contains(comparisonSearchQuery, ignoreCase = true)

            val matchFilter = when (comparisonFilterMode) {
                0 -> item.diffUnits != 0 || item.isManuallyAdjusted
                1 -> item.isManuallyAdjusted
                else -> true
            }

            matchSearch && matchFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stock_opname_comparison_tab"),
        contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TOP BANNER: PENJELASAN PEMBANDINGAN REAL-TIME
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaCreamCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(FarmivaMintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = FarmivaPrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Pembandingan Perubahan Data (Live)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmivaForestGreen
                            )
                            Text(
                                text = "Melihat langsung perbandingan data sistem vs fisik sebelum dan sesudah penyesuaian",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Export Buttons Row
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // PDF Button
                        Button(
                            onClick = {
                                try {
                                    val pdfFile = DinasReportHelper.generateStockOpnameComparisonPdf(
                                        context = context,
                                        profile = pharmacyProfile,
                                        currentUser = currentUser,
                                        items = reconciliationList,
                                        notes = opnameNotes
                                    )
                                    DinasReportHelper.sharePdf(
                                        context = context,
                                        file = pdfFile,
                                        dialogTitle = "Bagikan Berkas Pembandingan Opname (PDF)"
                                    )
                                    showToast("Berkas PDF pembandingan berhasil dibuat!")
                                } catch (e: Exception) {
                                    showToast("Gagal membuat PDF: ${e.localizedMessage}")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("download_comparison_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Excel Button
                        Button(
                            onClick = {
                                val xmlContent = onExportXmlString(reconciliationList)
                                val fileName = "Pembandingan_Opname_${System.currentTimeMillis()}.xls"
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = fileName,
                                    fileContent = xmlContent,
                                    mimeType = "application/vnd.ms-excel",
                                    dialogTitle = "Buka Pembandingan di Excel"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("download_comparison_excel_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4 Metric KPI Cards for Comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Card 1: Valuasi Sistem Sebelum
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FarmivaCreamSecondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SEBELUM (Sistem)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatRp(totalSystemValuation), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen, maxLines = 1)
                            Text("$totalSKUs SKU", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Card 2: Valuasi Fisik Sesudah
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FarmivaLightMint,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SESUDAH (Fisik)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen)
                            Text(formatRp(totalPhysicalValuation), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen, maxLines = 1)
                            Text("${itemsWithChange.size} berubah", fontSize = 8.sp, color = FarmivaForestGreen)
                        }
                    }

                    // Card 3: Delta Finansial (HPP)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (totalNetValuationDiff < 0) AlertExpiringRed.copy(alpha = 0.1f) else Color(0xFF1E88E5).copy(alpha = 0.1f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SELISIH FINANSIAL", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatRp(totalNetValuationDiff),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalNetValuationDiff < 0) AlertExpiringRed else Color(0xFF1E88E5),
                                maxLines = 1
                            )
                            Text(
                                text = if (totalNetValuationDiff < 0) "Kerugian HPP" else if (totalNetValuationDiff > 0) "Surplus Aset" else "Seimbang (0)",
                                fontSize = 8.sp,
                                color = if (totalNetValuationDiff < 0) AlertExpiringRed else Color(0xFF1E88E5)
                            )
                        }
                    }

                    // Card 4: Penyesuaian Manual
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MANUAL ADJUST", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            Text("${manuallyAdjustedItems.size} SKU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Text("Input manual", fontSize = 8.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        }
    }

    // SEARCH & FILTER CONTROLS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = comparisonSearchQuery,
                    onValueChange = { comparisonSearchQuery = it },
                    placeholder = { Text("Cari item obat pembandingan...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FarmivaWhite,
                        unfocusedContainerColor = FarmivaWhite
                    ),
                    singleLine = true
                )

                // Finalize Audit Button
                Button(
                    onClick = onCommitAudit,
                    colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("commit_audit_from_comparison_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = comparisonFilterMode == 0,
                    onClick = { comparisonFilterMode = 0 },
                    label = { Text("Yang Ada Perubahan (${itemsWithChange.size})", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmivaMintSoft,
                        selectedLabelColor = FarmivaForestGreen
                    )
                )
                FilterChip(
                    selected = comparisonFilterMode == 1,
                    onClick = { comparisonFilterMode = 1 },
                    label = { Text("Penyesuaian Manual (${manuallyAdjustedItems.size})", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF3C7),
                        selectedLabelColor = Color(0xFF92400E)
                    )
                )
                FilterChip(
                    selected = comparisonFilterMode == 2,
                    onClick = { comparisonFilterMode = 2 },
                    label = { Text("Semua ($totalSKUs)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmivaMintSoft,
                        selectedLabelColor = FarmivaForestGreen
                    )
                )
            }
        }

        // COMPARISON ITEMS LIST
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FarmivaPrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada item dengan filter pembandingan terpilih.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gunakan tombol 'Input Selisih Manual' di tab Audit untuk menyesuaikan stok.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.batch.id }) { item ->
                    val isShortage = item.diffUnits < 0
                    val isSurplus = item.diffUnits > 0
                    val isMatched = item.diffUnits == 0
                    val isManual = item.isManuallyAdjusted

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isManual -> Color(0xFFFFFBEB)
                                isShortage -> Color(0xFFFFF5F5)
                                isSurplus -> Color(0xFFF0F9FF)
                                else -> FarmivaCreamCard
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isManual -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                                isShortage -> AlertExpiringRed.copy(alpha = 0.4f)
                                isSurplus -> Color(0xFF1E88E5).copy(alpha = 0.4f)
                                else -> FarmivaCreamBorder
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comparison_card_${item.batch.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.drug.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = FarmivaForestGreen
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = FarmivaMintSoft,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = item.drug.category.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FarmivaForestGreen,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Batch: ${item.batch.batchNumber} | Exp: ${item.batch.expiryDate} | HPP Beli: ${formatRp(item.drug.buyPrice)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Status Badges
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isManual) {
                                        Surface(
                                            color = Color(0xFFF59E0B),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "MANUAL",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = when {
                                            isShortage -> AlertExpiringRed
                                            isSurplus -> Color(0xFF1E88E5)
                                            else -> FarmivaPrimaryGreen
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isShortage -> "SELISIH KURANG"
                                                isSurplus -> "SELISIH LEBIH"
                                                else -> "SESUAI"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Manual Adjustment Details (if any)
                            if (isManual) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Alasan Penyesuaian: ${item.manualAdjustment?.reason}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = "Alokasi: ${item.manualAdjustment?.targetLocation}  •  Catatan: ${item.manualAdjustment?.notes?.ifBlank { "Tidak ada" } ?: "Tidak ada"}",
                                                fontSize = 8.5.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }

                                        Text(
                                            text = "Delta: ${if ((item.manualAdjustment?.manualDiff ?: 0) > 0) "+" else ""}${item.manualAdjustment?.manualDiff}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if ((item.manualAdjustment?.manualDiff ?: 0) < 0) AlertExpiringRed else Color(0xFF1E88E5)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = FarmivaCreamBorder
                            )

                            // 3-COLUMN SIDE-BY-SIDE COMPARISON
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. SEBELUM (Database Sistem)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(FarmivaCreamSecondary, RoundedCornerShape(6.dp))
                                        .padding(6.dp)
                                ) {
                                    Text("SEBELUM (Sistem)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Etalase: ${item.batch.etalaseStock}", fontSize = 9.5.sp)
                                    Text("Gudang: ${item.batch.gudangStock}", fontSize = 9.5.sp)
                                    Text("Total: ${item.systemTotal} ${item.drug.unit}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen)
                                    Text("Nilai: ${formatRp(item.systemValuation)}", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // 2. PERUBAHAN (Delta Live)
                                Column(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .background(
                                            if (isShortage) AlertExpiringRed.copy(alpha = 0.1f)
                                            else if (isSurplus) Color(0xFF1E88E5).copy(alpha = 0.1f)
                                            else FarmivaLightMint,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("PERUBAHAN (Delta)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${if (item.diffUnits > 0) "+" else ""}${item.diffUnits} ${item.drug.unit}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = when {
                                            isShortage -> AlertExpiringRed
                                            isSurplus -> Color(0xFF1E88E5)
                                            else -> FarmivaPrimaryGreen
                                        }
                                    )
                                    Text(
                                        text = formatRp(item.valuationDiff),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isShortage -> AlertExpiringRed
                                            isSurplus -> Color(0xFF1E88E5)
                                            else -> FarmivaPrimaryGreen
                                        }
                                    )
                                    Text(
                                        text = if (isShortage) "Kerugian HPP" else if (isSurplus) "Surplus Stok" else "Akurat (Klop)",
                                        fontSize = 8.sp,
                                        color = if (isShortage) AlertExpiringRed else if (isSurplus) Color(0xFF1E88E5) else FarmivaPrimaryGreen
                                    )
                                }

                                // 3. SESUDAH (Hasil Fisik & Rekonsiliasi)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(FarmivaLightMint, RoundedCornerShape(6.dp))
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("SESUDAH (Fisik)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen)
                                    Text("Etalase: ${item.physicalEtalase}", fontSize = 9.5.sp)
                                    Text("Gudang: ${item.physicalGudang}", fontSize = 9.5.sp)
                                    Text("Total: ${item.physicalTotal} ${item.drug.unit}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen)
                                    Text("Nilai: ${formatRp(item.physicalValuation)}", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = FarmivaForestGreen)
                                }
                            }

                            // Quick Edit & Reset Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.diffUnits != 0 || isManual) {
                                    TextButton(onClick = { onResetItemToSystem(item) }) {
                                        Text("Reset ke Sistem (0 Selisih)", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                OutlinedButton(
                                    onClick = { onEditManualItem(item) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmivaForestGreen)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Edit Selisih Manual", fontSize = 9.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
