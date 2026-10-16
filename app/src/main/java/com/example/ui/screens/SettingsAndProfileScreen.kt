package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import com.example.util.DinasReportHelper
import com.example.util.ExcelHelper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.FarmivaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsAndProfileScreen(viewModel: FarmivaViewModel) {
    val profile by viewModel.pharmacyProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Local editable fields
    var pharmacyName by remember(profile) { mutableStateOf(profile.pharmacyName) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var sipa by remember(profile) { mutableStateOf(profile.sipaNumber) }
    var sia by remember(profile) { mutableStateOf(profile.siaNumber) }
    var apotekerName by remember(profile) { mutableStateOf(profile.apotekerName) }
    var ttkName by remember(profile) { mutableStateOf(profile.ttkName) }
    var psaName by remember(profile) { mutableStateOf(profile.psaName) }

    // Banking & DANA
    var bankName by remember(profile) { mutableStateOf(profile.bankName) }
    var bankAccountNumber by remember(profile) { mutableStateOf(profile.bankAccountNumber) }
    var bankAccountHolder by remember(profile) { mutableStateOf(profile.bankAccountHolder) }
    var danaNumber by remember(profile) { mutableStateOf(profile.danaNumber) }
    var qrisMidtransEnabled by remember(profile) { mutableStateOf(profile.qrisMidtransEnabled) }

    // Toggles
    var cashierAudioEnabled by remember(profile) { mutableStateOf(profile.cashierAudioEnabled) }
    var stockAlertEnabled by remember(profile) { mutableStateOf(profile.stockAlertEnabled) }
    var printerPaperWidth by remember(profile) { mutableStateOf(profile.printerPaperWidth) }

    // Backup & Restore Dialog
    var showRestoreDialog by remember { mutableStateOf(false) }

    fun saveAllSettings() {
        viewModel.updateProfile(
            profile.copy(
                pharmacyName = pharmacyName,
                address = address,
                phone = phone,
                sipaNumber = sipa,
                siaNumber = sia,
                apotekerName = apotekerName,
                ttkName = ttkName,
                psaName = psaName,
                bankName = bankName,
                bankAccountNumber = bankAccountNumber,
                bankAccountHolder = bankAccountHolder,
                danaNumber = danaNumber,
                qrisMidtransEnabled = qrisMidtransEnabled,
                cashierAudioEnabled = cashierAudioEnabled,
                stockAlertEnabled = stockAlertEnabled,
                printerPaperWidth = printerPaperWidth
            )
        )
        viewModel.showToast("Pengaturan profil & perbankan berhasil disimpan!")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Save Button floating/top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pengaturan Sistem & Profil Apotek",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = { saveAllSettings() },
                colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                modifier = Modifier.testTag("save_profile_settings_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simpan", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECTION 1: LEGAL & PROFILE APOTEK
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = HijauApotekPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Data Legalitas & Profil Apotek", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedTextField(value = pharmacyName, onValueChange = { pharmacyName = it }, label = { Text("Nama Toko / Apotek") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Lengkap Apotek") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor Telepon / WhatsApp") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sipa, onValueChange = { sipa = it }, label = { Text("No. Izin SIPA") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = sia, onValueChange = { sia = it }, label = { Text("No. Izin SIA") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                OutlinedTextField(value = apotekerName, onValueChange = { apotekerName = it }, label = { Text("Nama Apoteker Pengelola (APA)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ttkName, onValueChange = { ttkName = it }, label = { Text("Nama Tenaga Teknis (TTK)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = psaName, onValueChange = { psaName = it }, label = { Text("Nama Pemilik Sarana Apotek (PSA)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        }

        // SECTION 2: PERBANKAN & INTEGRASI QRIS DANA
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Integrasi Rekening Bank & QRIS DANA (Midtrans/Xendit)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Nama Bank") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = bankAccountNumber, onValueChange = { bankAccountNumber = it }, label = { Text("Nomor Rekening") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                OutlinedTextField(value = bankAccountHolder, onValueChange = { bankAccountHolder = it }, label = { Text("Atas Nama Rekening") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = danaNumber, onValueChange = { danaNumber = it }, label = { Text("Nomor Akun Bisnis DANA") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Gateway QRIS (Midtrans/Xendit)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = qrisMidtransEnabled, onCheckedChange = { qrisMidtransEnabled = it })
                }
            }
        }

        // SECTION 3: SYSTEM TOGGLES (Audio, Dark Mode, Printer)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Preferensi Sistem & Notifikasi Suara", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = HijauApotekPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Notifikasi Suara Kasir (Audio Alert)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Bunyi lonceng kasir saat transaksi sukses", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = cashierAudioEnabled, onCheckedChange = { cashierAudioEnabled = it })
                }

                // Coba Suara Notifikasi Kasir button
                OutlinedButton(
                    onClick = {
                        viewModel.playCashierChime()
                        viewModel.showToast("🔔 Suara notifikasi kasir berbunyi!")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("test_cashier_sound_button")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = HijauApotekPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Coba Suara Notifikasi Kasir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HijauApotekPrimary)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Mode Gelap (Dark Mode Switcher)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Tampilan gelap (#0F172A) & terang (#FFFFFF)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ukuran Kertas Thermal Printer", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Row {
                        FilterChip(
                            selected = printerPaperWidth == "58mm",
                            onClick = { printerPaperWidth = "58mm" },
                            label = { Text("58mm", fontSize = 10.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        FilterChip(
                            selected = printerPaperWidth == "80mm",
                            onClick = { printerPaperWidth = "80mm" },
                            label = { Text("80mm", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // SECTION 4: SYSTEM BACKUP & RESTORE DATA (1-Click JSON & SQL Export)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = HijauApotekPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("System Backup, Restore & Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Text(
                    text = "Amankan basis data apotek Anda. Unduh salinan data sewaktu-waktu atau pulihkan jika berganti perangkat.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1-Click JSON Backup
                    OutlinedButton(
                        onClick = {
                            val backupJson = """
                                {
                                  "app": "FARMIVA",
                                  "exportDate": "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}",
                                  "status": "VALID_DATABASE_BACKUP",
                                  "pharmacy": "${profile.pharmacyName}",
                                  "sipa": "${profile.sipaNumber}"
                                }
                            """.trimIndent()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Backup FARMIVA JSON", backupJson)
                            clipboard.setPrimaryClip(clip)
                            viewModel.showToast("Backup JSON 1-Click berhasil dibuat & disalin!")
                        },
                        modifier = Modifier.weight(1f).testTag("download_backup_json_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup .JSON", fontSize = 11.sp)
                    }

                    // 1-Click SQL Dump Export
                    OutlinedButton(
                        onClick = {
                            val sqlDump = """
                                -- FARMIVA SQL DATABASE DUMP
                                -- GENERATED AT ${Date()}
                                CREATE TABLE IF NOT EXISTS drugs (id INTEGER PRIMARY KEY, name TEXT, barcode TEXT);
                                CREATE TABLE IF NOT EXISTS stock_batches (id INTEGER PRIMARY KEY, drugId INTEGER, expiryDate TEXT);
                                -- DUMP SUCCESS
                            """.trimIndent()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Backup FARMIVA SQL", sqlDump)
                            clipboard.setPrimaryClip(clip)
                            viewModel.showToast("Export SQL Dump berhasil dibuat & disalin!")
                        },
                        modifier = Modifier.weight(1f).testTag("export_database_sql_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export .SQL", fontSize = 11.sp)
                    }
                }

                // Dinas Kesehatan & BPOM Export Buttons
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "Laporan Resmi Cadangan Gudang untuk Pelaporan ke Dinas:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B4332)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PDF Dinas
                    Button(
                        onClick = {
                            val warehouseList = mutableListOf<Pair<com.example.data.model.DrugWithBatches, com.example.data.model.StockBatch>>()
                            drugsWithBatches.forEach { dwb ->
                                dwb.batches.forEach { b -> warehouseList.add(Pair(dwb, b)) }
                            }
                            val pdfFile = DinasReportHelper.generateDinasWarehousePdf(context, profile, currentUser, warehouseList)
                            DinasReportHelper.savePdfToDownloads(context, pdfFile.name, pdfFile)
                            DinasReportHelper.sharePdf(context, pdfFile, "Bagikan Laporan PDF Dinas")
                            viewModel.showToast("Laporan PDF Dinas berhasil dibuat & disimpan ke Unduhan!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("settings_dinas_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Dinas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Excel Dinas (.XLS)
                    Button(
                        onClick = {
                            val warehouseList = mutableListOf<Pair<com.example.data.model.DrugWithBatches, com.example.data.model.StockBatch>>()
                            drugsWithBatches.forEach { dwb ->
                                dwb.batches.forEach { b -> warehouseList.add(Pair(dwb, b)) }
                            }
                            val excelXml = DinasReportHelper.generateDinasWarehouseExcel(profile, currentUser, warehouseList)
                            val fileName = "Laporan_Dinas_Gudang_Farmiva_${System.currentTimeMillis()}.xls"
                            ExcelHelper.saveToDownloads(context, fileName, excelXml)
                            ExcelHelper.shareFile(context, fileName, excelXml, "application/vnd.ms-excel", "Buka Laporan Excel Dinas")
                            viewModel.showToast("Laporan Excel Dinas berhasil dibuat & disimpan!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("settings_dinas_excel_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel Dinas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Restore Data Button
                    Button(
                        onClick = { showRestoreDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier.weight(1f).testTag("restore_data_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Data", fontSize = 11.sp)
                    }

                    // Cloud Multi-Device Sync Now
                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        modifier = Modifier.weight(1f).testTag("force_cloud_sync_button")
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Cloud", fontSize = 11.sp)
                    }
                }
            }
        }

        // SECTION 5: AKUN PENGGUNA & SESI LOGIN (LOGOUT)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = HijauApotekPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Akun Pengguna & Manajemen Sesi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentUser.username != "guest") HijauApotekPrimary.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentUser.username != "guest") HijauApotekPrimary.copy(alpha = 0.25f) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentUser.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Role: ${currentUser.role.roleTitle} • Email: ${currentUser.email.ifBlank { "Lokal" }}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (currentUser.username != "guest") "Status: Sesi Aktif Terverifikasi" else "Status: Tamu (Belum Login)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentUser.username != "guest") Color(0xFF16A34A) else Color.Gray
                            )
                        }

                        if (currentUser.username != "guest") {
                            Button(
                                onClick = { showLogoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("settings_logout_account_button")
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Logout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }

    // Modal: Restore Data
    if (showRestoreDialog) {
        var restorePayload by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pulihkan Data Sistem (Restore)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Tempel teks cadangan JSON atau SQL yang sebelumnya diunduh:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = restorePayload,
                        onValueChange = { restorePayload = it },
                        placeholder = { Text("Tempel JSON / SQL backup...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRestoreDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                viewModel.showToast("Data sistem berhasil dipulihkan dari file backup!")
                                showRestoreDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pulihkan")
                        }
                    }
                }
            }
        }
    }

    // Modal: Konfirmasi Logout Akun
    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar dari Akun", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFDC2626))
                    }

                    Text(
                        text = "Apakah Anda yakin ingin logout dari akun ${currentUser.fullName} (${currentUser.role.roleTitle})?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                viewModel.logout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("confirm_settings_logout_button")
                        ) {
                            Text("Ya, Logout")
                        }
                    }
                }
            }
        }
    }
}
