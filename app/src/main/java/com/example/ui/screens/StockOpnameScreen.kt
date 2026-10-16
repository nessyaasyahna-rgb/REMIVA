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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.OpnameDiscrepancyItem
import com.example.data.model.StockOpnameRecord
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
import com.example.data.model.ManualOpnameAdjustment
import com.example.ui.theme.FarmivaSageAccent
import com.example.ui.theme.FarmivaWhite
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.FarmivaViewModel
import com.example.util.ExcelHelper
import com.example.util.DinasReportHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OpnameFilterMode(val label: String) {
    ALL("Semua Item"),
    DISCREPANCY_ONLY("Ada Selisih Saja"),
    MATCHING_ONLY("Sesuai (Klop)"),
    SHORTAGE_ONLY("Selisih Kurang (Hilang/Rusak)"),
    SURPLUS_ONLY("Selisih Lebih")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOpnameScreen(viewModel: FarmivaViewModel) {
    val context = LocalContext.current
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()
    val opnameRecords by viewModel.opnames.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pharmacyProfile by viewModel.pharmacyProfile.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Form Audit Aktif, 1: Pembandingan Perubahan Live, 2: Riwayat & Arsip
    var opnameNotes by remember { mutableStateOf("Stock opname periodik rekonsiliasi etalase vs gudang") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(OpnameFilterMode.ALL) }

    // Dialog state for Discrepancy Report modal
    var showReportModal by remember { mutableStateOf(false) }
    var selectedHistoricalRecord by remember { mutableStateOf<StockOpnameRecord?>(null) }
    var showConfirmSaveDialog by remember { mutableStateOf(false) }

    // Manual adjustment modal state
    var itemForManualAdjust by remember { mutableStateOf<OpnameDiscrepancyItem?>(null) }

    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    // Discrepancy inputs map: batchId -> Pair(physicalEtalase, physicalGudang)
    val physicalInputs = remember { mutableStateMapOf<Long, Pair<Int, Int>>() }

    // Manual adjustment map: batchId -> ManualOpnameAdjustment
    val manualAdjustments = remember { mutableStateMapOf<Long, ManualOpnameAdjustment>() }

    // Prepare full reconciliation list
    val reconciliationList = remember(drugsWithBatches, physicalInputs.toMap(), manualAdjustments.toMap()) {
        val list = mutableListOf<OpnameDiscrepancyItem>()
        drugsWithBatches.forEach { drugItem ->
            drugItem.batches.forEach { batch ->
                val currentInputs = physicalInputs[batch.id] ?: Pair(batch.etalaseStock, batch.gudangStock)
                val manualAdj = manualAdjustments[batch.id]
                list.add(
                    OpnameDiscrepancyItem(
                        drug = drugItem.drug,
                        batch = batch,
                        physicalEtalase = currentInputs.first,
                        physicalGudang = currentInputs.second,
                        manualAdjustment = manualAdj
                    )
                )
            }
        }
        list
    }

    // Filtered list according to search & filter chip
    val filteredReconciliationList = remember(reconciliationList, searchQuery, selectedFilter) {
        reconciliationList.filter { item ->
            val matchSearch = searchQuery.isBlank() ||
                    item.drug.name.contains(searchQuery, ignoreCase = true) ||
                    item.batch.batchNumber.contains(searchQuery, ignoreCase = true) ||
                    item.drug.barcode.contains(searchQuery, ignoreCase = true) ||
                    item.drug.category.displayName.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                OpnameFilterMode.ALL -> true
                OpnameFilterMode.DISCREPANCY_ONLY -> item.diffUnits != 0
                OpnameFilterMode.MATCHING_ONLY -> item.diffUnits == 0
                OpnameFilterMode.SHORTAGE_ONLY -> item.diffUnits < 0
                OpnameFilterMode.SURPLUS_ONLY -> item.diffUnits > 0
            }

            matchSearch && matchFilter
        }
    }

    val totalSKUs = reconciliationList.size
    val totalDiffUnits = reconciliationList.sumOf { it.diffUnits }
    val totalLossValue = reconciliationList.sumOf { it.lossValue }
    val totalSurplusUnits = reconciliationList.filter { it.diffUnits > 0 }.sumOf { it.diffUnits }
    val totalShortageUnits = reconciliationList.filter { it.diffUnits < 0 }.sumOf { -it.diffUnits }
    val totalDiscrepanciesCount = reconciliationList.count { it.diffUnits != 0 }
    val totalMatchingCount = reconciliationList.count { it.diffUnits == 0 }

    // Function to generate Excel XML report for discrepancies
    fun exportDiscrepancyReportXml(items: List<OpnameDiscrepancyItem>): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"""")
            appendLine(""" xmlns:o="urn:schemas-microsoft-com:office:office"""")
            appendLine(""" xmlns:x="urn:schemas-microsoft-com:office:excel"""")
            appendLine(""" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"""")
            appendLine(""" xmlns:html="http://www.w3.org/TR/REC-html40">""")
            appendLine(""" <Styles>""")
            appendLine("""  <Style ss:ID="Header">""")
            appendLine("""   <Font ss:Bold="1" ss:Color="#FFFFFF"/>""")
            appendLine("""   <Interior ss:Color="#2D6A4F" ss:Pattern="Solid"/>""")
            appendLine("""   <Alignment ss:Horizontal="Center"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="Title">""")
            appendLine("""   <Font ss:Bold="1" ss:Size="14" ss:Color="#1B4332"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="Shortage">""")
            appendLine("""   <Font ss:Bold="1" ss:Color="#D90429"/>""")
            appendLine("""   <Interior ss:Color="#FCE4EC" ss:Pattern="Solid"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="Surplus">""")
            appendLine("""   <Font ss:Bold="1" ss:Color="#1E88E5"/>""")
            appendLine("""   <Interior ss:Color="#E3F2FD" ss:Pattern="Solid"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="Matched">""")
            appendLine("""   <Interior ss:Color="#E8F5E9" ss:Pattern="Solid"/>""")
            appendLine("""  </Style>""")
            appendLine(""" </Styles>""")
            appendLine(""" <Worksheet ss:Name="Laporan Selisih Opname">""")
            appendLine("""  <Table>""")
            appendLine("""   <Column ss:Width="110"/>""")
            appendLine("""   <Column ss:Width="160"/>""")
            appendLine("""   <Column ss:Width="90"/>""")
            appendLine("""   <Column ss:Width="80"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="90"/>""")
            appendLine("""   <Column ss:Width="110"/>""")
            appendLine("""   <Column ss:Width="90"/>""")

            // Title rows
            appendLine("""   <Row><Cell ss:StyleID="Title"><Data ss:Type="String">LAPORAN HASIL STOCK OPNAME &amp; REKONSILIASI</Data></Cell></Row>""")
            appendLine("""   <Row><Cell><Data ss:Type="String">${pharmacyProfile.pharmacyName} - ${pharmacyProfile.address}</Data></Cell></Row>""")
            appendLine("""   <Row><Cell><Data ss:Type="String">Tanggal Audit: ${SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())} | Auditor: ${currentUser.fullName}</Data></Cell></Row>""")
            appendLine("""   <Row><Cell><Data ss:Type="String">Ringkasan: Total Discrepancy: $totalDiffUnits unit | Nilai Kerugian: ${formatRp(totalLossValue)}</Data></Cell></Row>""")
            appendLine("""   <Row/>""")

            // Table Header
            appendLine("""   <Row ss:StyleID="Header">""")
            listOf(
                "Barcode", "Nama Obat / Item", "Kategori", "No Batch", "Kadaluwarsa",
                "Sist Etalase", "Sist Gudang", "Sist Total",
                "Fisik Etalase", "Fisik Gudang", "Fisik Total",
                "Selisih (Unit)", "Nilai Kerugian", "Status Rekonsiliasi"
            ).forEach { col ->
                appendLine("""    <Cell><Data ss:Type="String">$col</Data></Cell>""")
            }
            appendLine("""   </Row>""")

            // Rows
            items.forEach { item ->
                val style = when {
                    item.diffUnits < 0 -> " ss:StyleID=\"Shortage\""
                    item.diffUnits > 0 -> " ss:StyleID=\"Surplus\""
                    else -> " ss:StyleID=\"Matched\""
                }
                val statusText = when {
                    item.diffUnits < 0 -> "Kurang (${item.diffUnits})"
                    item.diffUnits > 0 -> "Lebih (+${item.diffUnits})"
                    else -> "Sesuai / Klop"
                }
                appendLine("""   <Row>""")
                appendLine("""    <Cell><Data ss:Type="String">${item.drug.barcode}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${item.drug.name}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${item.drug.category.displayName}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${item.batch.batchNumber}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${item.batch.expiryDate}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${item.batch.etalaseStock}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${item.batch.gudangStock}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${item.systemTotal}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${item.physicalEtalase}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${item.physicalGudang}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${item.physicalTotal}</Data></Cell>""")
                appendLine("""    <Cell$style><Data ss:Type="Number">${item.diffUnits}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${item.lossValue}</Data></Cell>""")
                appendLine("""    <Cell$style><Data ss:Type="String">$statusText</Data></Cell>""")
                appendLine("""   </Row>""")
            }

            appendLine("""  </Table>""")
            appendLine(""" </Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }

    // Function to generate CSV report
    fun exportDiscrepancyReportCsv(items: List<OpnameDiscrepancyItem>): String {
        return buildString {
            append('\uFEFF') // UTF-8 BOM
            appendLine("Barcode,Nama Obat,Kategori,No Batch,Kadaluwarsa,Sist Etalase,Sist Gudang,Sist Total,Fisik Etalase,Fisik Gudang,Fisik Total,Selisih Unit,Harga Beli Satuan,Nilai Kerugian,Status")
            items.forEach { item ->
                val statusText = when {
                    item.diffUnits < 0 -> "KURANG"
                    item.diffUnits > 0 -> "LEBIH"
                    else -> "SESUAI"
                }
                appendLine("\"${item.drug.barcode}\",\"${item.drug.name}\",\"${item.drug.category.displayName}\",\"${item.batch.batchNumber}\",${item.batch.expiryDate},${item.batch.etalaseStock},${item.batch.gudangStock},${item.systemTotal},${item.physicalEtalase},${item.physicalGudang},${item.physicalTotal},${item.diffUnits},${item.drug.buyPrice},${item.lossValue},$statusText")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FarmivaCreamBg)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Stock Opname & Rekonsiliasi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmivaForestGreen
                )
                Text(
                    text = "Verifikasi fisik stok vs database sistem secara akurat",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Generate Discrepancy Report Button
            Button(
                onClick = { showReportModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("open_discrepancy_report_modal")
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Laporan Selisih", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Tab Switcher (0: Audit Fisik, 1: Pembandingan Perubahan Live, 2: Riwayat Audit)
        val changedCount = reconciliationList.count { it.diffUnits != 0 || it.isManuallyAdjusted }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Audit Fisik ($totalSKUs)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmivaMintSoft,
                    selectedLabelColor = FarmivaForestGreen
                ),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                label = { Text("Pembandingan ($changedCount)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFEF3C7),
                    selectedLabelColor = Color(0xFF92400E)
                ),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                label = { Text("Riwayat (${opnameRecords.size})", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmivaMintSoft,
                    selectedLabelColor = FarmivaForestGreen
                ),
                modifier = Modifier.weight(1f)
            )
        }

        if (activeTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("stock_opname_active_tab_list"),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Calculation Engine Summary Banner
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (totalLossValue > 0) AlertExpiringRed.copy(alpha = 0.08f) else FarmivaCreamCard
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (totalLossValue > 0) AlertExpiringRed.copy(alpha = 0.3f) else FarmivaCreamBorder
                        ),
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
                                            .size(24.dp)
                                            .background(
                                                if (totalLossValue > 0) AlertExpiringRed.copy(alpha = 0.15f) else FarmivaSageAccent.copy(alpha = 0.2f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (totalLossValue > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (totalLossValue > 0) AlertExpiringRed else FarmivaPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Engine Rekonsiliasi Otomatis (Live)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FarmivaForestGreen
                                    )
                                }

                                // Reset All Counts Button
                                TextButton(
                                    onClick = {
                                        physicalInputs.clear()
                                        manualAdjustments.clear()
                                        viewModel.showToast("Semua hitungan fisik di-reset ke nilai default sistem")
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Reset Fisik", fontSize = 10.sp, color = FarmivaForestGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 4 Stat Metric Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Metric 1: Sesuai / Klop
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FarmivaLightMint,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Sesuai (Klop)", fontSize = 9.sp, color = FarmivaForestGreen)
                                        Text("$totalMatchingCount SKU", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FarmivaPrimaryGreen)
                                    }
                                }

                                // Metric 2: Selisih Kurang
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (totalShortageUnits > 0) AlertExpiringRed.copy(alpha = 0.12f) else FarmivaCreamSecondary,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Kurang", fontSize = 9.sp, color = if (totalShortageUnits > 0) AlertExpiringRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("-$totalShortageUnits unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (totalShortageUnits > 0) AlertExpiringRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Metric 3: Selisih Lebih
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (totalSurplusUnits > 0) Color(0xFF1E88E5).copy(alpha = 0.12f) else FarmivaCreamSecondary,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Lebih", fontSize = 9.sp, color = if (totalSurplusUnits > 0) Color(0xFF1E88E5) else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("+$totalSurplusUnits unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (totalSurplusUnits > 0) Color(0xFF1E88E5) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Metric 4: Nilai Kerugian
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (totalLossValue > 0) AlertExpiringRed.copy(alpha = 0.15f) else FarmivaCreamSecondary,
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Nilai Kerugian", fontSize = 9.sp, color = if (totalLossValue > 0) AlertExpiringRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatRp(totalLossValue), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (totalLossValue > 0) AlertExpiringRed else FarmivaPrimaryGreen)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Search Bar & Filter Options
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari obat, no batch, atau barcode...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FarmivaWhite,
                                unfocusedContainerColor = FarmivaWhite,
                                focusedBorderColor = FarmivaPrimaryGreen
                            ),
                            singleLine = true
                        )
                    }
                }

                // 3. Quick Filter Chips row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OpnameFilterMode.values().forEach { filter ->
                            val isSelected = selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (filter == OpnameFilterMode.DISCREPANCY_ONLY || filter == OpnameFilterMode.SHORTAGE_ONLY)
                                        AlertExpiringRed.copy(alpha = 0.2f)
                                    else FarmivaMintSoft,
                                    selectedLabelColor = if (filter == OpnameFilterMode.DISCREPANCY_ONLY || filter == OpnameFilterMode.SHORTAGE_ONLY)
                                        AlertExpiringRed
                                    else FarmivaForestGreen
                                ),
                                modifier = Modifier.padding(0.dp)
                            )
                        }
                    }
                }

                // 4. Opname Notes & Commit Button Row
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = FarmivaCreamCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = opnameNotes,
                                onValueChange = { opnameNotes = it },
                                placeholder = { Text("Catatan / Alasan Stock Opname...", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = FarmivaWhite,
                                    unfocusedContainerColor = FarmivaWhite
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = { showConfirmSaveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("commit_stock_opname_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simpan & Finalisasi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 5. Item Reconciliation Table (Scrollable List)
                if (filteredReconciliationList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FarmivaPrimaryGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tidak ada item obat dengan filter terpilih.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredReconciliationList, key = { it.batch.id }) { item ->
                        val isShortage = item.diffUnits < 0
                        val isSurplus = item.diffUnits > 0
                        val isMatched = item.diffUnits == 0

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isShortage -> Color(0xFFFFF5F5)
                                    isSurplus -> Color(0xFFF0F9FF)
                                    else -> FarmivaCreamCard
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isShortage -> AlertExpiringRed.copy(alpha = 0.4f)
                                    isSurplus -> Color(0xFF1E88E5).copy(alpha = 0.4f)
                                    else -> FarmivaCreamBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("opname_item_${item.batch.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Header: Drug Name + Category + Status Chip
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
                                            text = "Batch: ${item.batch.batchNumber}  |  Exp: ${item.batch.expiryDate}  |  Barcode: ${item.drug.barcode}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (item.isManuallyAdjusted) {
                                            Surface(
                                                color = Color(0xFFF59E0B),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "MANUAL: ${item.manualAdjustment?.reason?.take(16)}",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Status Badge
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
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = FarmivaCreamBorder
                                )

                                // Grid of System vs Physical Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Data Sistem
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(FarmivaCreamSecondary, RoundedCornerShape(6.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text("SISTEM (Database)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Etalase: ${item.batch.etalaseStock}", fontSize = 10.sp)
                                        Text("Gudang: ${item.batch.gudangStock}", fontSize = 10.sp)
                                        Text("Total: ${item.systemTotal} ${item.drug.unit}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen)
                                    }

                                    // 2. Fisik Input: Etalase
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Fisik Etalase", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = FarmivaPrimaryGreen)
                                        OutlinedTextField(
                                            value = item.physicalEtalase.toString(),
                                            onValueChange = { newVal ->
                                                val qty = newVal.toIntOrNull() ?: 0
                                                physicalInputs[item.batch.id] = Pair(qty.coerceAtLeast(0), item.physicalGudang)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = FarmivaWhite,
                                                unfocusedContainerColor = FarmivaWhite
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("input_physical_etalase_${item.batch.id}"),
                                            singleLine = true
                                        )
                                    }

                                    // 3. Fisik Input: Gudang
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Fisik Gudang", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F766E))
                                        OutlinedTextField(
                                            value = item.physicalGudang.toString(),
                                            onValueChange = { newVal ->
                                                val qty = newVal.toIntOrNull() ?: 0
                                                physicalInputs[item.batch.id] = Pair(item.physicalEtalase, qty.coerceAtLeast(0))
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = FarmivaWhite,
                                                unfocusedContainerColor = FarmivaWhite
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("input_physical_gudang_${item.batch.id}"),
                                            singleLine = true
                                        )
                                    }

                                    // 4. Otomatisasi Selisih & Kerugian
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
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Selisih Fisik", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${if (item.diffUnits > 0) "+" else ""}${item.diffUnits} ${item.drug.unit}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when {
                                                isShortage -> AlertExpiringRed
                                                isSurplus -> Color(0xFF1E88E5)
                                                else -> FarmivaPrimaryGreen
                                            }
                                        )
                                        if (item.lossValue > 0) {
                                            Text(
                                                text = "Rugi: ${formatRp(item.lossValue)}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AlertExpiringRed
                                            )
                                        } else if (isSurplus) {
                                            Text(
                                                text = "Lebih: +${item.diffUnits}",
                                                fontSize = 9.sp,
                                                color = Color(0xFF1E88E5)
                                            )
                                        } else {
                                            Text("Akurat / Klop", fontSize = 9.sp, color = FarmivaPrimaryGreen)
                                        }
                                    }
                                }

                                // Quick actions: Input Selisih Manual & Reset
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { itemForManualAdjust = item },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmivaForestGreen),
                                        modifier = Modifier.testTag("open_manual_diff_${item.batch.id}")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(if (item.isManuallyAdjusted) "Edit Selisih Manual" else "Input Selisih Manual (+/-)", fontSize = 10.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.diffUnits != 0 || item.isManuallyAdjusted) {
                                            TextButton(
                                                onClick = {
                                                    // Make physical match system
                                                    physicalInputs[item.batch.id] = Pair(item.batch.etalaseStock, item.batch.gudangStock)
                                                    manualAdjustments.remove(item.batch.id)
                                                    viewModel.showToast("Stok ${item.drug.name} di-reset ke nilai sistem")
                                                }
                                            ) {
                                                Text("Reset ke Sistem (0 Selisih)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeTab == 1) {
            // TAB 1: PEMBANDINGAN PERUBAHAN DATA (LIVE)
            StockOpnameComparisonTab(
                reconciliationList = reconciliationList,
                pharmacyProfile = pharmacyProfile,
                currentUser = currentUser,
                opnameNotes = opnameNotes,
                onEditManualItem = { selectedItem ->
                    itemForManualAdjust = selectedItem
                },
                onResetItemToSystem = { targetItem ->
                    physicalInputs[targetItem.batch.id] = Pair(targetItem.batch.etalaseStock, targetItem.batch.gudangStock)
                    manualAdjustments.remove(targetItem.batch.id)
                    viewModel.showToast("Item ${targetItem.drug.name} di-reset ke nilai sistem")
                },
                onCommitAudit = {
                    showConfirmSaveDialog = true
                },
                onExportXmlString = { items ->
                    exportDiscrepancyReportXml(items)
                },
                showToast = { msg ->
                    viewModel.showToast(msg)
                }
            )
        } else {
            // TAB 2: RIWAYAT & ARSIP LAPORAN OPNAME
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Audit Stock Opname Tersimpan (${opnameRecords.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaForestGreen
                    )
                }

                if (opnameRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = FarmivaSageAccent, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum ada riwayat stock opname yang tersimpan.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lakukan audit pada tab 'Audit Fisik Aktif' dan simpan hasilnya.",
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
                        items(opnameRecords) { record ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
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
                                        Column {
                                            Text(record.dateFormatted, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FarmivaForestGreen)
                                            Text("Auditor: ${record.auditorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            color = FarmivaPrimaryGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = record.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FarmivaPrimaryGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = FarmivaCreamBorder
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Selisih Unit:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${if (record.totalDiscrepancyUnits > 0) "+" else ""}${record.totalDiscrepancyUnits} unit",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (record.totalDiscrepancyUnits < 0) AlertExpiringRed else FarmivaPrimaryGreen
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Nilai Kerugian Terhitung:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = formatRp(record.totalLossValuation),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (record.totalLossValuation > 0) AlertExpiringRed else FarmivaPrimaryGreen
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Catatan: ${record.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action buttons: Bagikan ringkasan audit
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val summaryText = buildString {
                                                    appendLine("=== BUKTI AUDIT STOCK OPNAME ===")
                                                    appendLine("Apotek: ${pharmacyProfile.pharmacyName}")
                                                    appendLine("Tanggal: ${record.dateFormatted}")
                                                    appendLine("Auditor: ${record.auditorName}")
                                                    appendLine("Selisih Unit: ${record.totalDiscrepancyUnits} unit")
                                                    appendLine("Kerugian Terhitung: ${formatRp(record.totalLossValuation)}")
                                                    appendLine("Catatan: ${record.notes}")
                                                    appendLine("Status: ${record.status}")
                                                }
                                                ExcelHelper.shareFile(
                                                    context = context,
                                                    fileName = "Audit_Opname_${record.timestamp}.txt",
                                                    fileContent = summaryText,
                                                    mimeType = "text/plain",
                                                    dialogTitle = "Bagikan Bukti Stock Opname"
                                                )
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmivaForestGreen)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Bagikan Bukti", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MODAL: AUTOMATED DISCREPANCY REPORT
    // ==========================================
    if (showReportModal) {
        val discrepancyItems = reconciliationList.filter { it.diffUnits != 0 }
        val matchingItems = reconciliationList.filter { it.diffUnits == 0 }

        Dialog(onDismissRequest = { showReportModal = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaCreamCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Modal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(FarmivaMintSoft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = FarmivaPrimaryGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Laporan Selisih Stock Opname",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmivaForestGreen
                                )
                                Text(
                                    text = "Generated Automatically by System",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showReportModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FarmivaCreamBorder)

                    // Pharmacy & Audit Meta Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FarmivaCreamSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(pharmacyProfile.pharmacyName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FarmivaForestGreen)
                            Text("Waktu Pembuatan: ${SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())}", fontSize = 10.sp)
                            Text("Petugas Auditor: ${currentUser.fullName} (${currentUser.role.roleTitle})", fontSize = 10.sp)
                            Text("Total SKU Diperiksa: $totalSKUs batch obat", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculation Summary Card
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (totalLossValue > 0) AlertExpiringRed.copy(alpha = 0.08f) else FarmivaLightMint
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (totalLossValue > 0) AlertExpiringRed.copy(alpha = 0.3f) else FarmivaMintSoft
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Ringkasan Hasil Perhitungan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SKU Selisih (Discrepancy):", fontSize = 10.sp)
                                Text("$totalDiscrepanciesCount SKU", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (totalDiscrepanciesCount > 0) AlertExpiringRed else FarmivaPrimaryGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SKU Sesuai (Matched):", fontSize = 10.sp)
                                Text("$totalMatchingCount SKU", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = FarmivaPrimaryGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Selisih Barang:", fontSize = 10.sp)
                                Text("${if (totalDiffUnits > 0) "+" else ""}$totalDiffUnits Unit", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Estimasi Kerugian:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(formatRp(totalLossValue), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (totalLossValue > 0) AlertExpiringRed else FarmivaPrimaryGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Itemized Breakdown of Discrepancies
                    Text(
                        text = "Rincian Item Bermasalah (${discrepancyItems.size}):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaForestGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (discrepancyItems.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FarmivaLightMint,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FarmivaPrimaryGreen, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Sempurna! Semua item fisik klop 100% dengan data sistem.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmivaPrimaryGreen)
                            }
                        }
                    } else {
                        discrepancyItems.forEach { item ->
                            val isShortage = item.diffUnits < 0
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isShortage) AlertExpiringRed.copy(alpha = 0.06f) else Color(0xFF1E88E5).copy(alpha = 0.06f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isShortage) AlertExpiringRed.copy(alpha = 0.2f) else Color(0xFF1E88E5).copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.drug.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(
                                            text = "${if (item.diffUnits > 0) "+" else ""}${item.diffUnits} unit",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isShortage) AlertExpiringRed else Color(0xFF1E88E5)
                                        )
                                    }
                                    Text(
                                        text = "Batch: ${item.batch.batchNumber} | Sist: ${item.systemTotal} | Fisik: ${item.physicalTotal}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.lossValue > 0) {
                                        Text(
                                            text = "Kerugian HPP: ${formatRp(item.lossValue)} (Beli: ${formatRp(item.drug.buyPrice)}/u)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AlertExpiringRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Export / Download Options
                    Text("Ekspor Laporan Resmi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmivaForestGreen)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // PDF Dinas Report
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
                                        dialogTitle = "Bagikan Laporan Selisih Opname (PDF)"
                                    )
                                    viewModel.showToast("Berkas PDF Laporan Selisih berhasil dibuat!")
                                } catch (e: Exception) {
                                    viewModel.showToast("Gagal membuat PDF: ${e.localizedMessage}")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("PDF Resmi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Excel XML Export
                        Button(
                            onClick = {
                                val xmlContent = exportDiscrepancyReportXml(reconciliationList)
                                val fileName = "Laporan_Opname_FARMIVA_${System.currentTimeMillis()}.xls"
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = fileName,
                                    fileContent = xmlContent,
                                    mimeType = "application/vnd.ms-excel",
                                    dialogTitle = "Buka Laporan Opname di Excel"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Excel (.XLS)", fontSize = 10.sp)
                        }

                        // CSV Export
                        OutlinedButton(
                            onClick = {
                                val csvContent = exportDiscrepancyReportCsv(reconciliationList)
                                val fileName = "Laporan_Opname_FARMIVA_${System.currentTimeMillis()}.csv"
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = fileName,
                                    fileContent = csvContent,
                                    mimeType = "text/csv",
                                    dialogTitle = "Bagikan Laporan CSV"
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmivaForestGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("CSV", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Download to Local Storage Direct
                    Button(
                        onClick = {
                            val xmlContent = exportDiscrepancyReportXml(reconciliationList)
                            val fileName = "Laporan_Opname_FARMIVA_${System.currentTimeMillis()}.xls"
                            val savedPath = ExcelHelper.saveToDownloads(context, fileName, xmlContent)
                            viewModel.showToast("Laporan berhasil disimpan ke Unduhan: $savedPath")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmivaForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan ke Memori Perangkat (Download)", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: CONFIRM COMMIT STOCK OPNAME
    // ==========================================
    if (showConfirmSaveDialog) {
        Dialog(onDismissRequest = { showConfirmSaveDialog = false }) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaCreamCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = FarmivaPrimaryGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfirmasi Finalisasi Audit", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmivaForestGreen)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Apakah Anda yakin ingin menyelesaikan Stock Opname ini?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FarmivaCreamSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("• Total SKU: $totalSKUs batch", fontSize = 10.sp)
                            Text("• Item Selisih: $totalDiscrepanciesCount SKU", fontSize = 10.sp, color = if (totalDiscrepanciesCount > 0) AlertExpiringRed else FarmivaPrimaryGreen)
                            Text("• Net Selisih Unit: $totalDiffUnits unit", fontSize = 10.sp)
                            Text("• Nilai Kerugian: ${formatRp(totalLossValue)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (totalLossValue > 0) AlertExpiringRed else FarmivaPrimaryGreen)
                            Text("• Sistem akan otomatis memperbarui stok etalase & gudang sesuai hitungan fisik Anda.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showConfirmSaveDialog = false }) {
                            Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showConfirmSaveDialog = false
                                viewModel.commitStockOpname(reconciliationList, opnameNotes)
                                activeTab = 2 // Switch to History & Archive tab after saving
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ya, Simpan Audit")
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: MANUAL DISCREPANCY ADJUSTMENT
    // ==========================================
    itemForManualAdjust?.let { item ->
        ManualDiscrepancyDialog(
            item = item,
            onDismiss = { itemForManualAdjust = null },
            onApplyAdjustment = { manualDiff, targetLocation, reason, notes ->
                val batch = item.batch
                // Calculate new physical distribution
                val (newEtalase, newGudang) = when (targetLocation) {
                    "ETALASE" -> {
                        val et = (batch.etalaseStock + manualDiff).coerceAtLeast(0)
                        Pair(et, batch.gudangStock)
                    }
                    "GUDANG" -> {
                        val gd = (batch.gudangStock + manualDiff).coerceAtLeast(0)
                        Pair(batch.etalaseStock, gd)
                    }
                    else -> { // PROPORSIONAL
                        val sysTotal = (batch.etalaseStock + batch.gudangStock).coerceAtLeast(1)
                        val etalaseShare = (batch.etalaseStock.toDouble() / sysTotal * manualDiff).toInt()
                        val gudangShare = manualDiff - etalaseShare
                        Pair(
                            (batch.etalaseStock + etalaseShare).coerceAtLeast(0),
                            (batch.gudangStock + gudangShare).coerceAtLeast(0)
                        )
                    }
                }

                physicalInputs[batch.id] = Pair(newEtalase, newGudang)
                manualAdjustments[batch.id] = ManualOpnameAdjustment(
                    batchId = batch.id,
                    manualDiff = manualDiff,
                    targetLocation = targetLocation,
                    reason = reason,
                    notes = notes
                )
                itemForManualAdjust = null
                viewModel.showToast("Penyesuaian manual ${item.drug.name} berhasil disimpan! Buka tab 'Pembandingan' untuk melihat perubahan.")
            }
        )
    }
}
