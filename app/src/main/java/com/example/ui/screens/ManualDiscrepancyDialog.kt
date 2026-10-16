package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ManualOpnameAdjustment
import com.example.data.model.OpnameDiscrepancyItem
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.AlertWarningAmber
import com.example.ui.theme.FarmivaCreamBorder
import com.example.ui.theme.FarmivaCreamCard
import com.example.ui.theme.FarmivaCreamSecondary
import com.example.ui.theme.FarmivaForestGreen
import com.example.ui.theme.FarmivaLightMint
import com.example.ui.theme.FarmivaMintSoft
import com.example.ui.theme.FarmivaPrimaryGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ManualDiscrepancyDialog(
    item: OpnameDiscrepancyItem,
    onDismiss: () -> Unit,
    onApplyAdjustment: (manualDiff: Int, targetLocation: String, reason: String, notes: String) -> Unit
) {
    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    val existingAdj = item.manualAdjustment
    var diffText by remember { mutableStateOf(existingAdj?.manualDiff?.toString() ?: item.diffUnits.toString()) }
    var targetLocation by remember { mutableStateOf(existingAdj?.targetLocation ?: "PROPORSIONAL") }
    var selectedReason by remember { mutableStateOf(existingAdj?.reason ?: "Barang Rusak / Pecah") }
    var customNotes by remember { mutableStateOf(existingAdj?.notes ?: "") }

    val parsedDiff = diffText.toIntOrNull() ?: 0
    val systemTotal = item.systemTotal
    val newPhysicalTotal = (systemTotal + parsedDiff).coerceAtLeast(0)
    val systemValuation = systemTotal * item.drug.buyPrice
    val newPhysicalValuation = newPhysicalTotal * item.drug.buyPrice
    val diffValuation = (newPhysicalTotal - systemTotal) * item.drug.buyPrice

    val quickReasons = listOf(
        "Barang Rusak / Pecah",
        "Kadaluwarsa di Rak",
        "Hilang / Salah Catat",
        "Bonus PBF Belum Tercatat",
        "Retur Pasien / Pelanggan",
        "Selisih Sampling Gudang",
        "Koreksi Hitung Fisik"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("manual_discrepancy_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FarmivaCreamCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FarmivaMintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = FarmivaPrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Input Selisih Manual",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmivaForestGreen
                            )
                            Text(
                                text = "Penyesuaian Fisik Langsung",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider(color = FarmivaCreamBorder)

                // Item Info Header
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FarmivaCreamSecondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = item.drug.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FarmivaForestGreen
                        )
                        Text(
                            text = "Batch: ${item.batch.batchNumber} | Exp: ${item.batch.expiryDate} | HPP Beli: ${formatRp(item.drug.buyPrice)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Stok Sistem: ${item.systemTotal} ${item.drug.unit} (Etalase: ${item.batch.etalaseStock}, Gudang: ${item.batch.gudangStock})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FarmivaForestGreen
                            )
                        }
                    }
                }

                // 1. INPUT SELISIH MANUAL ANGKA (+/-)
                Column {
                    Text(
                        text = "1. Masukkan Angka Selisih (+ atau -):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaForestGreen
                    )
                    Text(
                        text = "Contoh: Ketik -2 jika hilang/rusak 2 unit, atau ketik +3 jika lebih 3 unit.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrement Button
                        IconButton(
                            onClick = {
                                val current = diffText.toIntOrNull() ?: 0
                                diffText = (current - 1).toString()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FarmivaMintSoft)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurang 1", tint = AlertExpiringRed)
                        }

                        // TextField for Direct Input
                        OutlinedTextField(
                            value = diffText,
                            onValueChange = { newVal ->
                                // Allow negative sign and digits
                                if (newVal.isEmpty() || newVal == "-" || newVal.matches(Regex("^-?\\d+$"))) {
                                    diffText = newVal
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_diff_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Increment Button
                        IconButton(
                            onClick = {
                                val current = diffText.toIntOrNull() ?: 0
                                diffText = (current + 1).toString()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FarmivaMintSoft)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah 1", tint = Color(0xFF16A34A))
                        }
                    }

                    // Quick Shortcut Delta Chips
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(-10, -5, -2, -1, 0, 1, 2, 5, 10).forEach { delta ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (parsedDiff == delta) FarmivaPrimaryGreen else FarmivaMintSoft,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { diffText = delta.toString() }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (delta > 0) "+$delta" else "$delta",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (parsedDiff == delta) Color.White else FarmivaForestGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. TARGET LOKASI PENYESUAIAN
                Column {
                    Text(
                        text = "2. Alokasi Lokasi Penyesuaian:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaForestGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PROPORSIONAL" to "Proporsional (Otomatis)",
                            "ETALASE" to "Rak Etalase",
                            "GUDANG" to "Gudang Farmasi"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = targetLocation == key,
                                onClick = { targetLocation = key },
                                label = { Text(label, fontSize = 9.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FarmivaMintSoft,
                                    selectedLabelColor = FarmivaForestGreen
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. ALASAN PENYESUAIAN
                Column {
                    Text(
                        text = "3. Alasan Terjadinya Selisih:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaForestGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            quickReasons.take(4).forEach { r ->
                                FilterChip(
                                    selected = selectedReason == r,
                                    onClick = { selectedReason = r },
                                    label = { Text(r, fontSize = 9.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FarmivaMintSoft,
                                        selectedLabelColor = FarmivaForestGreen
                                    )
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            quickReasons.drop(4).forEach { r ->
                                FilterChip(
                                    selected = selectedReason == r,
                                    onClick = { selectedReason = r },
                                    label = { Text(r, fontSize = 9.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FarmivaMintSoft,
                                        selectedLabelColor = FarmivaForestGreen
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. CATATAN TAMBAHAN
                Column {
                    Text(
                        text = "4. Catatan / No. Berita Acara (Opsional):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaForestGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        placeholder = { Text("Keterangan tambahan petugas auditor...", fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                // 5. LIVE PREVIEW PEMBANDINGAN PERUBAHAN
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (parsedDiff < 0) AlertExpiringRed.copy(alpha = 0.08f)
                        else if (parsedDiff > 0) Color(0xFF1E88E5).copy(alpha = 0.08f)
                        else FarmivaLightMint
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (parsedDiff < 0) AlertExpiringRed.copy(alpha = 0.3f)
                        else if (parsedDiff > 0) Color(0xFF1E88E5).copy(alpha = 0.3f)
                        else FarmivaMintSoft
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = if (parsedDiff < 0) AlertExpiringRed else if (parsedDiff > 0) Color(0xFF1E88E5) else FarmivaPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pratinjau Live Perubahan Pembandingan:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmivaForestGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("SEBELUM (Database)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$systemTotal ${item.drug.unit}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(formatRp(systemValuation), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SELISIH (Delta)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${if (parsedDiff > 0) "+" else ""}$parsedDiff ${item.drug.unit}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (parsedDiff < 0) AlertExpiringRed else if (parsedDiff > 0) Color(0xFF1E88E5) else FarmivaPrimaryGreen
                                )
                                Text(
                                    text = formatRp(diffValuation),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (parsedDiff < 0) AlertExpiringRed else if (parsedDiff > 0) Color(0xFF1E88E5) else FarmivaPrimaryGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SESUDAH (Fisik Baru)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$newPhysicalTotal ${item.drug.unit}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FarmivaForestGreen)
                                Text(formatRp(newPhysicalValuation), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = FarmivaForestGreen)
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onApplyAdjustment(parsedDiff, targetLocation, selectedReason, customNotes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("apply_manual_adjustment_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terapkan & Bandingkan Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
