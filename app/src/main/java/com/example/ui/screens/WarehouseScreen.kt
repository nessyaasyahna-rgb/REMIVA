package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.util.ExcelHelper
import com.example.util.DinasReportHelper
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.text.NumberFormat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DrugWithBatches
import com.example.data.model.StockBatch
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.FarmivaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WarehouseScreen(viewModel: FarmivaViewModel) {
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()
    val mutations by viewModel.mutations.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pharmacyProfile by viewModel.pharmacyProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Stok Gudang & Audit Checklist, 1: Riwayat Mutasi
    var showDinasPreviewModal by remember { mutableStateOf(false) }

    // Interactive Checklist state (audit physically checked item keys: "batchId")
    val checkedAuditItems = remember { mutableStateMapOf<Long, Boolean>() }

    // Mutation Modal State
    var targetMutationBatch by remember { mutableStateOf<Pair<DrugWithBatches, StockBatch>?>(null) }
    var mutateQtyText by remember { mutableStateOf("5") }
    var mutateNoteText by remember { mutableStateOf("Pengisian rak kasir etalase") }

    // Delete Warehouse Batch State
    var targetDeleteWarehouseBatch by remember { mutableStateOf<Pair<DrugWithBatches, StockBatch>?>(null) }
    var warehouseDeleteReason by remember { mutableStateOf("Data Ganda (Duplikasi)") }

    // Flatten all warehouse batches
    val warehouseEntries = remember(drugsWithBatches, searchQuery) {
        val query = searchQuery.trim().lowercase()
        val list = mutableListOf<Pair<DrugWithBatches, StockBatch>>()
        drugsWithBatches.forEach { drugItem ->
            drugItem.batches.forEach { batch ->
                if (batch.gudangStock > 0 || query.isNotEmpty()) {
                    val match = query.isEmpty() ||
                            drugItem.drug.name.lowercase().contains(query) ||
                            drugItem.drug.barcode.lowercase().contains(query) ||
                            batch.batchNumber.lowercase().contains(query)
                    if (match) {
                        list.add(Pair(drugItem, batch))
                    }
                }
            }
        }
        list
    }

    val totalWarehouseUnits = remember(warehouseEntries) { warehouseEntries.sumOf { it.second.gudangStock } }
    val totalWarehouseValuation = remember(warehouseEntries) { warehouseEntries.sumOf { it.first.drug.buyPrice * it.second.gudangStock } }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Tab selector: Stok Gudang & Audit vs Riwayat Mutasi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Stok Gudang & Checklist Audit (${warehouseEntries.size})", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Warehouse, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Log Riwayat Mutasi (${mutations.size})", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("warehouse_items_list"),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari barang gudang (nama, barcode, no batch)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("warehouse_search_input"),
                        singleLine = true
                    )
                }

                // Audit checklist progress banner
                item {
                    val checkedCount = checkedAuditItems.count { it.value }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Checklist Fisik: $checkedCount / ${warehouseEntries.size} Diperiksa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (checkedCount > 0) {
                            Text(
                                text = "Reset Checklist",
                                fontSize = 11.sp,
                                color = Color.Red,
                                modifier = Modifier.clickable { checkedAuditItems.clear() }
                            )
                        }
                    }
                }

                // PELAPORAN RESMI DINAS KESEHATAN & BPOM CARD
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HijauApotekPrimary.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HijauApotekPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = HijauApotekPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Laporan Resmi Dinas Kesehatan & BPOM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1B4332)
                            )
                        }

                        // Preview Modal Button
                        OutlinedButton(
                            onClick = { showDinasPreviewModal = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("preview_dinas_report_button")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview Berkas", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Siap dibawa untuk audit/pelaporan dinas: ${warehouseEntries.size} SKU Batch • $totalWarehouseUnits Unit Fisik • Kop Apotek & Pengesahan APA",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3 Download Action Buttons (PDF, Excel, CSV)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Download PDF Dinas
                        Button(
                            onClick = {
                                val pdfFile = DinasReportHelper.generateDinasWarehousePdf(
                                    context = context,
                                    profile = pharmacyProfile,
                                    currentUser = currentUser,
                                    warehouseItems = warehouseEntries
                                )
                                val savedPath = DinasReportHelper.savePdfToDownloads(context, pdfFile.name, pdfFile)
                                DinasReportHelper.sharePdf(context, pdfFile, "Buka / Bagikan Berkas PDF Dinas")
                                viewModel.showToast("Laporan PDF Dinas berhasil dibuat & disimpan ke Unduhan: ${pdfFile.name}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(38.dp)
                                .testTag("download_dinas_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Download Excel (.XLS)
                        Button(
                            onClick = {
                                val excelXml = DinasReportHelper.generateDinasWarehouseExcel(
                                    profile = pharmacyProfile,
                                    currentUser = currentUser,
                                    warehouseItems = warehouseEntries
                                )
                                val fileName = "Laporan_Gudang_Dinas_FARMIVA_${System.currentTimeMillis()}.xls"
                                ExcelHelper.saveToDownloads(context, fileName, excelXml)
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = fileName,
                                    fileContent = excelXml,
                                    mimeType = "application/vnd.ms-excel",
                                    dialogTitle = "Buka Laporan Excel Dinas Kesehatan"
                                )
                                viewModel.showToast("Laporan Excel Dinas (${warehouseEntries.size} item) berhasil diekspor!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(38.dp)
                                .testTag("download_dinas_excel_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Backup CSV
                        OutlinedButton(
                            onClick = {
                                val csvData = buildString {
                                    append("\uFEFF") // UTF-8 BOM
                                    appendLine("BARCODE,NAMA_OBAT,KATEGORI,NO_BATCH,EXPIRED,STOK_GUDANG,SATUAN,HPP,HARGA_JUAL,SUPPLIER")
                                    warehouseEntries.forEach { (drugItem, batch) ->
                                        appendLine("${drugItem.drug.barcode},\"${drugItem.drug.name}\",\"${drugItem.drug.category.displayName}\",${batch.batchNumber},${batch.expiryDate},${batch.gudangStock},${drugItem.drug.unit},${drugItem.drug.buyPrice.toInt()},${drugItem.drug.sellPrice.toInt()},\"${batch.supplierName}\"")
                                    }
                                }
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = "backup_stok_gudang_farmiva.csv",
                                    fileContent = csvData,
                                    mimeType = "text/csv",
                                    dialogTitle = "Bagikan CSV Cadangan Gudang"
                                )
                                viewModel.showToast("Backup CSV data gudang berhasil disiapkan!")
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(38.dp)
                                .testTag("download_dinas_csv_button")
                        ) {
                            Text("CSV", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        items(warehouseEntries) { (drugItem, batch) ->
                    val isChecked = checkedAuditItems[batch.id] == true

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) HijauApotekPrimary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox for physical audit
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    checkedAuditItems[batch.id] = checked
                                    if (checked) {
                                        viewModel.showToast("Barang ${drugItem.drug.name} (Batch ${batch.batchNumber}) telah dicek fisik!")
                                    }
                                },
                                modifier = Modifier.testTag("audit_check_${batch.id}")
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = drugItem.drug.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Batch: ${batch.batchNumber} • Exp: ${batch.expiryDate} • ${drugItem.drug.unit}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lokasi: Rak Gudang Utama (${batch.supplierName})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${batch.gudangStock} Unit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (batch.gudangStock <= 5) Color.Red else HijauApotekPrimary
                                )
                                Text(
                                    text = "Etalase: ${batch.etalaseStock}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            targetDeleteWarehouseBatch = Pair(drugItem, batch)
                                            warehouseDeleteReason = "Data Ganda (Duplikasi)"
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp).testTag("delete_warehouse_batch_${batch.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(13.dp), tint = Color.Red)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Hapus", fontSize = 10.sp, color = Color.Red)
                                    }

                                    Button(
                                        onClick = {
                                            targetMutationBatch = Pair(drugItem, batch)
                                            mutateQtyText = minOf(batch.gudangStock, 10).toString()
                                        },
                                        enabled = batch.gudangStock > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp).testTag("mutate_button_${batch.id}")
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Mutasi", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // RIWAYAT MUTASI LOG (smooth scroll with ample bottom padding)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mutations.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada riwayat mutasi stok dari gudang ke etalase.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(mutations) { mut ->
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
                                    text = mut.drugName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = HijauApotekPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "+${mut.quantity} Unit Pindah",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HijauApotekPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("GUDANG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp).padding(horizontal = 2.dp))
                                Text("ETALASE KASIR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HijauApotekPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("• Batch: ${mut.batchNumber}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                text = "Petugas: ${mut.operatorName} • Catatan: ${mut.note}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(mut.timestamp)),
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Mutasi Stok Gudang -> Etalase
    targetMutationBatch?.let { (drugItem, batch) ->
        Dialog(onDismissRequest = { targetMutationBatch = null }) {
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
                        Text("Mutasi Stok ke Etalase", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { targetMutationBatch = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Text(
                        text = "${drugItem.drug.name} (Batch: ${batch.batchNumber})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Stok Tersedia di Gudang: ${batch.gudangStock} unit\nStok Saat ini di Etalase: ${batch.etalaseStock} unit",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = mutateQtyText,
                        onValueChange = { mutateQtyText = it },
                        label = { Text("Jumlah Unit yang Dipindahkan") },
                        modifier = Modifier.fillMaxWidth().testTag("mutate_quantity_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = mutateNoteText,
                        onValueChange = { mutateNoteText = it },
                        label = { Text("Catatan / Alasan Mutasi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val qty = mutateQtyText.toIntOrNull() ?: 1
                            if (qty > 0 && qty <= batch.gudangStock) {
                                viewModel.mutateStock(
                                    drugId = drugItem.drug.id,
                                    drugName = drugItem.drug.name,
                                    batchId = batch.id,
                                    batchNumber = batch.batchNumber,
                                    qty = qty,
                                    note = mutateNoteText
                                )
                                targetMutationBatch = null
                            } else {
                                viewModel.showToast("Jumlah tidak boleh melebihi stok gudang (${batch.gudangStock})")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_mutation_button")
                    ) {
                        Text("Konfirmasi Pemindahan Stok", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // MODAL: KONFIRMASI HAPUS STOK GUDANG (Data ganda, salah input, barang tidak lagi tersedia)
    if (targetDeleteWarehouseBatch != null) {
        val (drugItem, batch) = targetDeleteWarehouseBatch!!
        val deleteReasons = listOf(
            "Data Ganda (Duplikasi)",
            "Salah Input Jumlah / Data",
            "Barang Tidak Tersedia Lagi",
            "Barang Rusak / Kadaluarsa"
        )
        Dialog(onDismissRequest = { targetDeleteWarehouseBatch = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(22.dp))
                            Text("Hapus Stok Gudang", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Red)
                        }
                        IconButton(onClick = { targetDeleteWarehouseBatch = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = drugItem.drug.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Batch: ${batch.batchNumber} • Exp: ${batch.expiryDate}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Stok Gudang: ${batch.gudangStock} unit • Etalase: ${batch.etalaseStock} unit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Pilih Alasan Penghapusan dari Stok:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        deleteReasons.forEach { reason ->
                            val isSelected = warehouseDeleteReason == reason
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) BorderStroke(1.dp, Color.Red) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { warehouseDeleteReason = reason }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSelected) "● " else "○ ",
                                        color = if (isSelected) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = reason,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = Color.Red.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Data batch ini akan dihapus permanen dari inventaris stok apotek.",
                            fontSize = 10.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(8.dp),
                            lineHeight = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { targetDeleteWarehouseBatch = null },
                            modifier = Modifier.testTag("cancel_delete_warehouse_batch_button")
                        ) {
                            Text("Batal", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteBatch(batch, drugItem.drug.name, warehouseDeleteReason)
                                targetDeleteWarehouseBatch = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.testTag("confirm_delete_warehouse_batch_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus dari Gudang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // MODAL: PREVIEW BERKAS PELAPORAN DINAS KESEHATAN & BPOM
    if (showDinasPreviewModal) {
        val totalUnits = warehouseEntries.sumOf { it.second.gudangStock }
        val totalValuation = warehouseEntries.sumOf { it.first.drug.buyPrice * it.second.gudangStock }

        Dialog(onDismissRequest = { showDinasPreviewModal = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = HijauApotekPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preview Berkas Pelaporan Dinas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1B4332)
                            )
                        }
                        IconButton(onClick = { showDinasPreviewModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    // KOP SURAT RESMI
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("DINAS KESEHATAN & BALAI BESAR POM (BPOM)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            Text(pharmacyProfile.pharmacyName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B4332))
                            Text("No. SIA: ${pharmacyProfile.siaNumber}  |  No. SIPA: ${pharmacyProfile.sipaNumber}", fontSize = 9.sp, color = Color.DarkGray)
                            Text("Alamat: ${pharmacyProfile.address}", fontSize = 9.sp, color = Color.DarkGray)
                            Text("Apoteker Pengelola Apotek (APA): ${pharmacyProfile.apotekerName}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B4332))
                        }
                    }

                    // RINGKASAN DATA GUDANG
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Rekapitulasi Persediaan Gudang Farmasi:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Batch SKU:", fontSize = 10.sp)
                                Text("${warehouseEntries.size} Batch", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Unit Fisik:", fontSize = 10.sp)
                                Text("$totalUnits Unit", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Valuasi Nilai Aset HPP:", fontSize = 10.sp)
                                val rpValStr = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalValuation).replace("Rp", "Rp ")
                                Text(rpValStr, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HijauApotekPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Petugas Pemeriksa:", fontSize = 10.sp)
                                Text("${currentUser.fullName} (${currentUser.role.roleTitle})", fontSize = 10.sp)
                            }
                        }
                    }

                    // PREVIEW DAFTAR 3 ITEM PERTAMA
                    Text("Contoh Item Pelaporan (3 dari ${warehouseEntries.size} SKU):", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    warehouseEntries.take(3).forEach { (drugItem, batch) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(drugItem.drug.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Batch: ${batch.batchNumber} • Exp: ${batch.expiryDate} • ${drugItem.drug.category.displayName}", fontSize = 9.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${batch.gudangStock} ${drugItem.drug.unit}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F766E))
                                    Text("Gudang", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // LEMBAR PENGESAHAN PREVIEW
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Lembar Validasi & Cap Stempel:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Text("Dibuat oleh: ${currentUser.fullName}  |  Disetujui: ${pharmacyProfile.apotekerName} (APA)", fontSize = 9.sp, color = Color.DarkGray)
                            Text("Dokumen sah format A4 multi-halaman siap cetak/unduh.", fontSize = 9.sp, color = Color(0xFF15803D))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ACTION BUTTONS IN MODAL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val pdfFile = DinasReportHelper.generateDinasWarehousePdf(
                                    context = context,
                                    profile = pharmacyProfile,
                                    currentUser = currentUser,
                                    warehouseItems = warehouseEntries
                                )
                                DinasReportHelper.savePdfToDownloads(context, pdfFile.name, pdfFile)
                                DinasReportHelper.sharePdf(context, pdfFile, "Buka Berkas PDF Dinas")
                                viewModel.showToast("PDF Berkas Dinas berhasil diunduh!")
                                showDinasPreviewModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download PDF", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val excelXml = DinasReportHelper.generateDinasWarehouseExcel(
                                    profile = pharmacyProfile,
                                    currentUser = currentUser,
                                    warehouseItems = warehouseEntries
                                )
                                val fileName = "Laporan_Gudang_Dinas_FARMIVA_${System.currentTimeMillis()}.xls"
                                ExcelHelper.saveToDownloads(context, fileName, excelXml)
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = fileName,
                                    fileContent = excelXml,
                                    mimeType = "application/vnd.ms-excel",
                                    dialogTitle = "Buka Laporan Excel Dinas"
                                )
                                viewModel.showToast("Excel Laporan Dinas berhasil diunduh!")
                                showDinasPreviewModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download Excel", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
