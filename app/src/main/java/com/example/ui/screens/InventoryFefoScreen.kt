package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DrugCategory
import com.example.data.model.DrugWithBatches
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.AlertWarningOrange
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.FarmivaViewModel
import com.example.util.ExcelHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InventoryFefoScreen(viewModel: FarmivaViewModel) {
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("SEMUA") } // "SEMUA", "LOW_STOCK", "EXPIRING", "FAST_MOVING"
    var showAddDrugModal by remember { mutableStateOf(false) }
    var showTemplateModal by remember { mutableStateOf(false) }
    var showImportModal by remember { mutableStateOf(false) }
    var drugToDelete by remember { mutableStateOf<DrugWithBatches?>(null) }
    var batchToDelete by remember { mutableStateOf<Pair<com.example.data.model.DrugItem, com.example.data.model.StockBatch>?>(null) }
    var selectedDeleteReason by remember { mutableStateOf("Data Ganda (Duplikasi)") }

    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    val filteredList = drugsWithBatches.filter { item ->
        val query = searchQuery.trim().lowercase()
        val matchSearch = query.isEmpty() ||
                item.drug.name.lowercase().contains(query) ||
                item.drug.barcode.lowercase().contains(query) ||
                item.batches.any { it.batchNumber.lowercase().contains(query) }

        val matchFilter = when (selectedFilter) {
            "LOW_STOCK" -> item.isLowStock
            "EXPIRING" -> item.batches.any { it.expiryDate <= "2027-01-31" }
            "FAST_MOVING" -> item.drug.isFastMoving
            else -> true
        }
        matchSearch && matchFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("inventory_fefo_list"),
        contentPadding = PaddingValues(bottom = 120.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Action Bar: Download Template, Bulk Import, Tambah Obat
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { showTemplateModal = true },
                    modifier = Modifier.weight(1f).testTag("download_excel_template_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Template Excel", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showImportModal = true },
                    modifier = Modifier.weight(1f).testTag("bulk_import_excel_button")
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Excel", fontSize = 11.sp)
                }

                Button(
                    onClick = { showAddDrugModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                    modifier = Modifier.testTag("add_new_drug_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Tambah", fontSize = 11.sp)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari obat atau batch...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Filter chips: Semua, Stok Menipis, Menuju Expired, Fast Moving
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "SEMUA",
                        onClick = { selectedFilter = "SEMUA" },
                        label = { Text("Semua (${drugsWithBatches.size})", fontSize = 11.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "LOW_STOCK",
                        onClick = { selectedFilter = "LOW_STOCK" },
                        label = { Text("Stok Menipis", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AlertWarningOrange, modifier = Modifier.size(14.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "EXPIRING",
                        onClick = { selectedFilter = "EXPIRING" },
                        label = { Text("Mendekati Expired (<3 bln)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AlertExpiringRed, modifier = Modifier.size(14.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "FAST_MOVING",
                        onClick = { selectedFilter = "FAST_MOVING" },
                        label = { Text("Fast-Moving", fontSize = 11.sp) }
                    )
                }
            }
        }

        // FEFO Explanation Notice
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = HijauApotekPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = HijauApotekPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Algoritma FEFO (First Expired, First Out): Batch obat dengan tanggal expired terdekat otomatis diprioritaskan di kasir.",
                        fontSize = 10.sp,
                        color = HijauApotekPrimary
                    )
                }
            }
        }

        // List of Drugs with their FEFO Batches (smooth scroll with ample bottom padding)
        items(filteredList) { item ->
            InventoryDrugCard(
                item = item,
                formatRp = ::formatRp,
                onDeleteDrug = {
                    drugToDelete = it
                    selectedDeleteReason = "Data Ganda (Duplikasi)"
                },
                onDeleteBatch = { drug, batch ->
                    batchToDelete = Pair(drug, batch)
                    selectedDeleteReason = "Data Ganda (Duplikasi)"
                }
            )
        }
    }

    var importInputText by remember { mutableStateOf("") }
    var selectedImportFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = ExcelHelper.readTextFromUri(context, uri)
            if (!content.isNullOrBlank()) {
                importInputText = content
                val name = uri.lastPathSegment ?: "file_excel_apotek"
                selectedImportFileName = name.substringAfterLast("/")
                viewModel.showToast("File Excel/CSV berhasil dimuat! Siap di-import.")
            } else {
                viewModel.showToast("Gagal membaca file atau file kosong.")
            }
        }
    }

    // Modal: Template Excel Download
    if (showTemplateModal) {
        val templateXml = remember { viewModel.repository.getExcelTemplateXml() }
        val templateCsv = remember { viewModel.repository.getExcelTemplateCsv() }
        var selectedFormat by remember { mutableStateOf("XLS") } // "XLS" or "CSV"

        Dialog(onDismissRequest = { showTemplateModal = false }) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = HijauApotekPrimary)
                            Text("Template Excel Stok Apotek", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        IconButton(onClick = { showTemplateModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Text(
                        text = "Gunakan template ini untuk input stok banyak sekaligus. Buka di Microsoft Excel, Google Sheets, atau WPS Office:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Format Toggle: Excel XML (.xls) vs Excel CSV (.csv)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFormat == "XLS",
                            onClick = { selectedFormat = "XLS" },
                            label = { Text("Microsoft Excel (.xls)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedFormat == "CSV",
                            onClick = { selectedFormat = "CSV" },
                            label = { Text("Excel CSV (.csv)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Explanation of Columns
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Kolom Standar Farmasi (11 Kolom):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = HijauApotekPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Kode/Barcode  •  2. Nama Obat  •  3. Kategori (Bebas/Keras/Alkes)\n4. Lokasi (Etalase/Gudang)  •  5. Batch No  •  6. Expired (YYYY-MM-DD)\n7. Jumlah Stok  •  8. Satuan  •  9. Beli (Rp)  •  10. Jual (Rp)  •  11. Min. Stok",
                                fontSize = 10.sp,
                                color = Color(0xFF334155),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Action 1: Open / Share with Microsoft Excel or Google Sheets
                    Button(
                        onClick = {
                            if (selectedFormat == "XLS") {
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = "template_stok_apotek_farmiva.xls",
                                    fileContent = templateXml,
                                    mimeType = "application/vnd.ms-excel",
                                    dialogTitle = "Buka atau Bagikan Template Excel"
                                )
                            } else {
                                ExcelHelper.shareFile(
                                    context = context,
                                    fileName = "template_stok_apotek_farmiva.csv",
                                    fileContent = templateCsv,
                                    mimeType = "text/csv",
                                    dialogTitle = "Buka atau Bagikan Template CSV"
                                )
                            }
                            viewModel.showToast("Membuka template untuk diedit di Excel / Sheets...")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("open_excel_template_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFormat == "XLS") "Buka / Bagikan File Excel (.xls)" else "Buka / Bagikan File CSV (.csv)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Action 2: Save directly to device Downloads folder
                    OutlinedButton(
                        onClick = {
                            val fileName = if (selectedFormat == "XLS") "template_stok_apotek_farmiva.xls" else "template_stok_apotek_farmiva.csv"
                            val content = if (selectedFormat == "XLS") templateXml else templateCsv
                            val savedPath = ExcelHelper.saveToDownloads(context, fileName, content)
                            viewModel.showToast("File berhasil disimpan ke folder Download: $fileName")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_excel_to_downloads_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan File ke Folder Download", fontSize = 12.sp)
                    }

                    // Action 3: Copy text to clipboard
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Template Stok Farmiva", templateCsv)
                            clipboard.setPrimaryClip(clip)
                            viewModel.showToast("Format CSV/Excel telah disalin ke clipboard!")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("copy_template_clipboard_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Format Teks ke Clipboard", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Modal: Upload / Bulk Import from Excel
    if (showImportModal) {
        Dialog(onDismissRequest = { showImportModal = false }) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = HijauApotekPrimary)
                            Text("Import Data Stok dari Excel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        IconButton(onClick = { showImportModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Text(
                        text = "Unggah file Excel (.xlsx, .xls) atau CSV/TSV hingga ribuan baris dari memori HP. Sistem otomatis mendeteksi kolom barcode, nama obat, kategori, satuan, stok, harga, batch, dan tanggal expired:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Big Action Button: Pick File from Phone
                    Button(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier.fillMaxWidth().testTag("pick_excel_file_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih File Excel (.xlsx / .xls) atau CSV (Kapasitas Besar)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (selectedImportFileName != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "File Terpilih: $selectedImportFileName",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ukuran data: ${importInputText.length / 1024} KB • Format terverifikasi dan siap diimpor massal.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }

                    // Preset Buttons for Instant Testing & Large Catalog Demonstration
                    Text(
                        text = "Atau muat contoh katalog obat siap pakai (uji coba instan):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                importInputText = """
                                    8999901001,Paracetamol 500mg Strip,Obat Bebas,Etalase,B-PCT2712,2027-12-31,100,Strip,5000,7500,20
                                    8999901002,Amoxicillin 500mg Kaplet,Obat Keras,Gudang,B-AMX2706,2027-06-30,50,Boks,35000,48000,10
                                    8999901003,CTM 4mg Tablet,Obat Bebas Terbatas,Etalase,B-CTM2801,2028-01-15,200,Strip,2000,3500,30
                                    8999901004,Alkohol 70% 100ml,Alat Kesehatan,Gudang,B-ALK2905,2029-05-20,30,Botol,8000,12000,5
                                    8999901005,Vitamin C 500mg IPI,Suplemen & Vitamin,Etalase,B-VTC2810,2028-10-10,80,Botol,6500,9000,15
                                    8999901006,Omeprazole 20mg Kapsul,Obat Keras,Etalase & Gudang,B-OMP2703,2027-03-15,60,Strip,6000,11000,15
                                    8999901007,Betadine Antiseptik 30ml,Obat Bebas,Etalase,B-BTD2711,2027-11-20,40,Botol,18000,25000,10
                                    8999901008,Sanmol Sirup Anak 60ml,Obat Bebas,Etalase,B-SNM2708,2027-08-14,35,Botol,16500,22000,10
                                    8999901009,Mylanta Sirup 150ml,Obat Bebas Terbatas,Etalase,B-MYL2710,2027-10-05,25,Botol,42000,54000,8
                                    8999901010,Kasa Steril Husada 16x16,Alat Kesehatan,Gudang,B-KAS2912,2029-12-31,50,Kotak,7000,10500,12
                                """.trimIndent()
                                selectedImportFileName = "katalog_10_obat.csv"
                                viewModel.showToast("Data 10 obat apotek dimuat!")
                            },
                            modifier = Modifier.weight(1f).testTag("load_demo_10_drugs_button")
                        ) {
                            Text("10 Obat", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importInputText = """
                                    8999902001,Asam Mefenamat 500mg,Obat Keras,Etalase,B-ASM2801,2028-05-15,120,Strip,4500,7000,20
                                    8999902002,Ibuprofen 400mg Tablet,Obat Bebas Terbatas,Etalase,B-IBU2803,2028-03-20,90,Strip,5500,8500,15
                                    8999902003,Cetirizine 10mg Strip,Obat Bebas Terbatas,Etalase,B-CTZ2807,2028-07-10,150,Strip,4000,7000,25
                                    8999902004,Lansoprazole 30mg Kapsul,Obat Keras,Gudang,B-LNS2809,2028-09-30,75,Strip,7000,12000,15
                                    8999902005,Antasida Doen Tablet,Obat Bebas,Etalase,B-ATD2811,2028-11-25,180,Strip,2500,4500,30
                                    8999902006,Dexamethasone 0.5mg,Obat Keras,Etalase,B-DEX2804,2028-04-12,200,Strip,1800,3500,40
                                    8999902007,Salbutamol 2mg Tablet,Obat Keras,Etalase,B-SLB2806,2028-06-18,100,Strip,2200,4000,20
                                    8999902008,Ambroxol 30mg Tablet,Obat Bebas Terbatas,Etalase,B-AMB2808,2028-08-22,140,Strip,2500,4500,25
                                    8999902009,Cefadroxil 500mg Kapsul,Obat Keras,Gudang,B-CFD2810,2028-10-15,80,Boks,55000,75000,10
                                    8999902010,Metformin 500mg Tablet,Obat Keras,Etalase,B-MTF2812,2028-12-05,250,Strip,2800,5000,50
                                    8999902011,Glimepiride 2mg Tablet,Obat Keras,Etalase,B-GLM2802,2028-02-14,110,Strip,6000,9500,20
                                    8999902012,Amlodipine 10mg Tablet,Obat Keras,Etalase,B-AML2805,2028-05-19,300,Strip,3500,6000,60
                                    8999902013,Captopril 25mg Tablet,Obat Keras,Etalase,B-CPT2807,2028-07-28,200,Strip,2000,4000,40
                                    8999902014,Candesartan 8mg Tablet,Obat Keras,Gudang,B-CND2809,2028-09-11,70,Strip,15000,24000,15
                                    8999902015,Simvastatin 20mg Tablet,Obat Keras,Etalase,B-SMV2811,2028-11-03,160,Strip,4500,7500,30
                                    8999902016,Allopurinol 100mg Tablet,Obat Keras,Etalase,B-ALP2801,2028-01-20,130,Strip,3000,5500,25
                                    8999902017,Neurobion Forte Tablet,Suplemen & Vitamin,Etalase,B-NRB2803,2028-03-15,100,Strip,38000,49000,20
                                    8999902018,Sangobion Kapsul,Suplemen & Vitamin,Etalase,B-SNG2806,2028-06-25,90,Strip,18000,24000,15
                                    8999902019,Enervon C Botol 30s,Suplemen & Vitamin,Etalase,B-ENV2808,2028-08-30,45,Botol,38000,49500,10
                                    8999902020,Curcuma FCT Tablet,Suplemen & Vitamin,Etalase,B-CRC2810,2028-10-14,120,Strip,12000,17000,20
                                    8999902021,Diapet Kapsul Strip,Obat Bebas,Etalase,B-DPT2812,2028-12-18,140,Strip,4500,7000,25
                                    8999902022,Oralit 200ml Sachet,Obat Bebas,Etalase,B-ORL2802,2028-02-28,300,Sachet,1000,2000,50
                                    8999902023,Polysilane Sirup 100ml,Obat Bebas,Etalase,B-PLS2804,2028-04-10,50,Botol,28000,36000,10
                                    8999902024,Promag Tablet Kunyah,Obat Bebas,Etalase,B-PMG2806,2028-06-08,200,Strip,7500,10500,30
                                    8999902025,Tolak Angin Cair 12s,Obat Bebas,Etalase,B-TLK2808,2028-08-16,80,Boks,42000,54000,15
                                    8999902026,Freshcare Citrus Roll On,Obat Bebas,Etalase,B-FSC2810,2028-10-22,60,Botol,13000,18000,15
                                    8999902027,Minyak Kayu Putih 60ml,Obat Bebas,Etalase,B-MKP2812,2028-12-12,70,Botol,22000,29000,15
                                    8999902028,Rivanol 100ml Larutan,Alat Kesehatan,Gudang,B-RVN2901,2029-01-15,40,Botol,5500,8500,10
                                    8999902029,Plester Hansaplast 10s,Alat Kesehatan,Etalase,B-HNS2903,2029-03-25,150,Amplop,4000,6500,25
                                    8999902030,Bioplacenton Salep 15g,Obat Bebas Terbatas,Etalase,B-BPL2805,2028-05-30,85,Tube,23000,31000,15
                                """.trimIndent()
                                selectedImportFileName = "katalog_30_obat_populer.csv"
                                viewModel.showToast("Data 30 obat apotek populer dimuat!")
                            },
                            modifier = Modifier.weight(1f).testTag("load_demo_30_drugs_button")
                        ) {
                            Text("30 Obat Populer", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val bulkList = StringBuilder()
                                bulkList.append("Kode,Nama Obat,Kategori,Satuan,Stok,Harga Beli,Harga Jual,No Batch,Kadaluarsa,Lokasi,Supplier\n")
                                val prefixes = listOf("B-PCT", "B-AMX", "B-CTM", "B-OMP", "B-ANT", "B-VTC", "B-CFD", "B-AML", "B-SMV", "B-CIP")
                                val categories = listOf("Obat Bebas", "Obat Keras", "Obat Bebas Terbatas", "Suplemen & Vitamin", "Alat Kesehatan")
                                val drugNames = listOf(
                                    "Paracetamol 500mg", "Amoxicillin 500mg", "CTM 4mg", "Omeprazole 20mg", "Antasida Doen",
                                    "Vitamin C 500mg IPI", "Cefadroxil 500mg", "Amlodipine 10mg", "Simvastatin 20mg", "Ciprofloxacin 500mg",
                                    "Ibuprofen 400mg", "Asam Mefenamat 500mg", "Metformin 500mg", "Glimepiride 2mg", "Captopril 25mg",
                                    "Candesartan 8mg", "Allopurinol 100mg", "Ambroxol 30mg", "Dexamethasone 0.5mg", "Salbutamol 2mg"
                                )
                                for (i in 1..100) {
                                    val name = drugNames[(i - 1) % drugNames.size] + " (Batch #$i)"
                                    val code = "899880${String.format(java.util.Locale.US, "%04d", i)}"
                                    val cat = categories[(i - 1) % categories.size]
                                    val batch = "${prefixes[(i - 1) % prefixes.size]}-202${7 + (i % 3)}"
                                    val expYear = 2027 + (i % 3)
                                    val expMonth = String.format(java.util.Locale.US, "%02d", 1 + (i % 12))
                                    val exp = "$expYear-$expMonth-15"
                                    val stock = 20 + (i * 7) % 250
                                    val buy = 3000.0 + (i * 500) % 25000
                                    val sell = buy * 1.35
                                    val loc = if (i % 2 == 0) "Etalase" else "Gudang"
                                    bulkList.append("$code,$name,$cat,Strip,$stock,${buy.toInt()},${sell.toInt()},$batch,$exp,$loc,PBF Kimia Farma\n")
                                }
                                importInputText = bulkList.toString()
                                selectedImportFileName = "katalog_100_obat_lengkap.csv"
                                viewModel.showToast("Data 100 obat apotek kapasitas besar dimuat!")
                            },
                            modifier = Modifier.weight(1.1f).testTag("load_demo_100_drugs_button")
                        ) {
                            Text("100 Obat (Katalog)", fontSize = 11.sp)
                        }
                    }

                    // Estimated item count indicator
                    val estimatedCount = remember(importInputText) {
                        if (importInputText.contains("<Row", ignoreCase = true)) {
                            Regex("<Row", RegexOption.IGNORE_CASE).findAll(importInputText).count().coerceAtLeast(1) - 1
                        } else {
                            importInputText.lineSequence()
                                .map { it.trim() }
                                .count { it.isNotBlank() && !it.startsWith("Kode", ignoreCase = true) && !it.startsWith("Barcode", ignoreCase = true) && !it.startsWith("No,", ignoreCase = true) && !it.startsWith("#") && !it.startsWith("---") }
                        }
                    }

                    if (estimatedCount > 0) {
                        Surface(
                            color = HijauApotekPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HijauApotekPrimary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = "Siap Impor $estimatedCount Baris Data Obat",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HijauApotekPrimary
                                    )
                                    Text(
                                        text = "Kapasitas besar aktif: parsing multi-format (.xlsx, .xls, .csv), deduplikasi cerdas, dan sinkronisasi multi-device.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Content preview toggle & editor
                    var showRawTextEditor by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showRawTextEditor) "Teks Mentah Data (Editor)" else "Pratinjau Data Impor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (showRawTextEditor) "Tutup Editor Teks" else "Lihat / Edit Teks Mentah",
                            fontSize = 11.sp,
                            color = HijauApotekPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showRawTextEditor = !showRawTextEditor }
                        )
                    }

                    if (showRawTextEditor) {
                        OutlinedTextField(
                            value = importInputText,
                            onValueChange = { importInputText = it },
                            placeholder = { Text("Isi data CSV atau Excel muncul di sini...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            maxLines = 10,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        )
                    } else if (importInputText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Cuplikan 3 baris pertama data:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                importInputText.lines().filter { it.isNotBlank() }.take(3).forEachIndexed { idx, line ->
                                    Text(
                                        text = "${idx + 1}. ${line.take(75)}${if (line.length > 75) "..." else ""}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    val isImporting by viewModel.isImporting.collectAsState()

                    // Final Submit Button
                    Button(
                        onClick = {
                            if (importInputText.isNotBlank()) {
                                viewModel.importCsvData(importInputText) {
                                    showImportModal = false
                                }
                            } else {
                                viewModel.showToast("Pilih file atau muat data terlebih dahulu.")
                            }
                        },
                        enabled = !isImporting,
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("process_import_button")
                    ) {
                        if (isImporting) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang Memproses & Menyinkronkan ke Cloud...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (estimatedCount > 0) "Proses & Simpan $estimatedCount Obat ke Stok" else "Proses & Simpan ke Stok Apotek",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Add New Drug Form
    if (showAddDrugModal) {
        var drugName by remember { mutableStateOf("") }
        var barcode by remember { mutableStateOf("") }
        var genericName by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(DrugCategory.BEBAS) }
        var indication by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("Strip") }
        var buyPrice by remember { mutableStateOf("") }
        var sellPrice by remember { mutableStateOf("") }
        var minStock by remember { mutableStateOf("10") }
        var batchNo by remember { mutableStateOf("B-FAR${(100..999).random()}") }
        var expiryDate by remember { mutableStateOf("2027-12-31") }
        var etalaseStock by remember { mutableStateOf("15") }
        var gudangStock by remember { mutableStateOf("30") }

        Dialog(onDismissRequest = { showAddDrugModal = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tambah Obat & Batch FEFO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showAddDrugModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    OutlinedTextField(value = drugName, onValueChange = { drugName = it }, label = { Text("Nama Obat (Merk Dagang)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Kode Barcode") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = indication, onValueChange = { indication = it }, label = { Text("Khasiat / Indikasi") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it }, label = { Text("Harga Beli (Rp)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = it }, label = { Text("Harga Jual (Rp)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = batchNo, onValueChange = { batchNo = it }, label = { Text("Nomor Batch") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = expiryDate, onValueChange = { expiryDate = it }, label = { Text("Expired (YYYY-MM-DD)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = etalaseStock, onValueChange = { etalaseStock = it }, label = { Text("Stok Etalase") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = gudangStock, onValueChange = { gudangStock = it }, label = { Text("Stok Gudang") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Button(
                        onClick = {
                            if (drugName.isNotBlank()) {
                                viewModel.addNewDrugWithBatch(
                                    name = drugName,
                                    barcode = barcode,
                                    genericName = genericName,
                                    category = category,
                                    indication = indication,
                                    unit = unit,
                                    buyPrice = buyPrice.toDoubleOrNull() ?: 5000.0,
                                    sellPrice = sellPrice.toDoubleOrNull() ?: 8000.0,
                                    minStock = minStock.toIntOrNull() ?: 10,
                                    isFastMoving = false,
                                    requirePrescription = category == DrugCategory.KERAS || category == DrugCategory.PSIKOTROPIKA,
                                    batchNumber = batchNo,
                                    expiryDate = expiryDate,
                                    etalaseStock = etalaseStock.toIntOrNull() ?: 10,
                                    gudangStock = gudangStock.toIntOrNull() ?: 20
                                )
                                showAddDrugModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Obat & Daftarkan ke FEFO", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal: Konfirmasi Hapus Seluruh Obat (Data ganda, salah input, barang tidak lagi tersedia)
    if (drugToDelete != null) {
        val targetDrug = drugToDelete!!
        val deleteReasons = listOf(
            "Data Ganda (Duplikasi)",
            "Salah Input / Kesalahan Pencatatan",
            "Barang Tidak Tersedia Lagi",
            "Barang Rusak / Kadaluarsa"
        )
        Dialog(onDismissRequest = { drugToDelete = null }) {
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
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AlertExpiringRed, modifier = Modifier.size(24.dp))
                            Text("Hapus Obat dari Stok", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AlertExpiringRed)
                        }
                        IconButton(onClick = { drugToDelete = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = targetDrug.drug.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Barcode: ${targetDrug.drug.barcode} • Kategori: ${targetDrug.drug.category.displayName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total Stok: ${targetDrug.totalStock} unit (${targetDrug.batches.size} batch terdaftar)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HijauApotekPrimary
                            )
                        }
                    }

                    Text(
                        text = "Pilih Alasan Penghapusan Data:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        deleteReasons.forEach { reason ->
                            val isSelected = selectedDeleteReason == reason
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AlertExpiringRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) BorderStroke(1.dp, AlertExpiringRed) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDeleteReason = reason }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSelected) "● " else "○ ",
                                        color = if (isSelected) AlertExpiringRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = reason,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AlertExpiringRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = AlertExpiringRed.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Data obat beserta seluruh batch-nya akan dihapus permanen dari etalase & gudang.",
                            fontSize = 10.sp,
                            color = AlertExpiringRed,
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
                            onClick = { drugToDelete = null },
                            modifier = Modifier.testTag("cancel_delete_drug_button")
                        ) {
                            Text("Batal", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteDrug(targetDrug.drug, selectedDeleteReason)
                                drugToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertExpiringRed),
                            modifier = Modifier.testTag("confirm_delete_drug_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Obat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal: Konfirmasi Hapus Batch Spesifik (Jika hanya satu batch yang salah input/ganda)
    if (batchToDelete != null) {
        val (drug, batch) = batchToDelete!!
        val deleteReasons = listOf(
            "Data Ganda (Duplikasi)",
            "Salah Input Data",
            "Barang Tidak Tersedia Lagi",
            "Barang Rusak / Kadaluarsa"
        )
        Dialog(onDismissRequest = { batchToDelete = null }) {
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
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AlertExpiringRed, modifier = Modifier.size(22.dp))
                            Text("Hapus Batch Obat", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AlertExpiringRed)
                        }
                        IconButton(onClick = { batchToDelete = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = drug.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Nomor Batch: ${batch.batchNumber} • Exp: ${batch.expiryDate}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Stok Etalase: ${batch.etalaseStock} unit | Gudang: ${batch.gudangStock} unit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Pilih Alasan Penghapusan Batch:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        deleteReasons.forEach { reason ->
                            val isSelected = selectedDeleteReason == reason
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AlertExpiringRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) BorderStroke(1.dp, AlertExpiringRed) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDeleteReason = reason }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSelected) "● " else "○ ",
                                        color = if (isSelected) AlertExpiringRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = reason,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AlertExpiringRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { batchToDelete = null },
                            modifier = Modifier.testTag("cancel_delete_batch_button")
                        ) {
                            Text("Batal", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteBatch(batch, drug.name, selectedDeleteReason)
                                batchToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertExpiringRed),
                            modifier = Modifier.testTag("confirm_delete_batch_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Batch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryDrugCard(
    item: DrugWithBatches,
    formatRp: (Double) -> String,
    onDeleteDrug: (DrugWithBatches) -> Unit,
    onDeleteBatch: (com.example.data.model.DrugItem, com.example.data.model.StockBatch) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Drug Header with Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.drug.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Barcode: ${item.drug.barcode} • ${item.drug.unit} • ${item.drug.indication}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(item.drug.category.badgeColorHex).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.drug.category.displayName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(item.drug.category.badgeColorHex),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Tombol Hapus Obat dari stok
                    IconButton(
                        onClick = { onDeleteDrug(item) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_drug_btn_${item.drug.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Obat",
                            tint = AlertExpiringRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing & Stock summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Harga Jual / Beli", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatRp(item.drug.sellPrice)} / ${formatRp(item.drug.buyPrice)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Etalase", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.totalEtalase} unit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HijauApotekPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gudang", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.totalGudang} unit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Stok", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${item.totalStock} unit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isLowStock) AlertWarningOrange else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FEFO Batches List with individual Batch Delete
            Text(
                text = "Daftar Batch (Urutan Pengeluaran FEFO):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            item.fefoBatches.forEachIndexed { index, batch ->
                val isExpiringSoon = batch.expiryDate <= "2027-01-31"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = if (index == 0) HijauApotekPrimary else Color.LightGray,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "#${index + 1}",
                                fontSize = 8.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Batch ${batch.batchNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exp: ${batch.expiryDate}",
                            fontSize = 10.sp,
                            fontWeight = if (isExpiringSoon) FontWeight.Bold else FontWeight.Normal,
                            color = if (isExpiringSoon) AlertExpiringRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "E:${batch.etalaseStock} | G:${batch.gudangStock}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Tombol hapus batch jika salah input / ganda
                        IconButton(
                            onClick = { onDeleteBatch(item.drug, batch) },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_batch_btn_${batch.id}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Batch",
                                tint = AlertExpiringRed.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
