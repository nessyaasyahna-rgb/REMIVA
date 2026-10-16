package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PharmacyProfile
import com.example.ui.theme.HijauApotekPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportPreviewDialog(
    reportTitle: String,
    reportSubtitle: String,
    csvRawData: String,
    profile: PharmacyProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val todayDate = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Modal header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KOP Surat & Laporan Resmi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Official Document Page Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        // KOP SURAT RESMI
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.farmiva_custom_logo_1791555234480),
                                contentDescription = "Logo KOP",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = profile.pharmacyName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = profile.address,
                                    fontSize = 9.sp,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "SIA: ${profile.siaNumber} | SIPA: ${profile.sipaNumber}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "Telp: ${profile.phone} | PSA: ${profile.psaName}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 2.dp,
                            color = Color(0xFF0F172A)
                        )

                        // Report Title
                        Text(
                            text = reportTitle.uppercase(Locale.getDefault()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = reportSubtitle,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Raw / Tabular Data Preview
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = csvRawData,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Official Signatures and Stamp Slots (APA & TTK)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: TTK
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Petugas Teknis (TTK),", fontSize = 10.sp, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(38.dp))
                                Text(
                                    text = profile.ttkName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text("SIPTTK Aktif", fontSize = 9.sp, color = Color(0xFF64748B))
                            }

                            // Center: Stempel Apotek
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "STEMPEL\nRESMI",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Right: APA
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Jakarta, $todayDate", fontSize = 9.sp, color = Color(0xFF475569))
                                Text("Apoteker Pengelola (APA),", fontSize = 10.sp, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(30.dp))
                                Text(
                                    text = profile.apotekerName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text("STRA / SIPA Aktif", fontSize = 9.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Copy Excel (.csv) & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(reportTitle, csvRawData)
                            clipboard.setPrimaryClip(clip)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "$reportTitle\n\n$csvRawData")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Laporan Resmi"))
                        },
                        modifier = Modifier.weight(1f).testTag("share_official_report_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.padding(end = 4.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(reportTitle, csvRawData)
                            clipboard.setPrimaryClip(clip)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        modifier = Modifier.weight(1f).testTag("download_excel_report_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.padding(end = 4.dp))
                        Text("Salin Excel (.CSV)", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
