package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PharmacyProfile
import com.example.data.model.Transaction
import com.example.ui.theme.FarmivaForestGreen
import com.example.ui.theme.FarmivaMintSoft
import com.example.ui.theme.FarmivaPrimaryGreen
import com.example.ui.theme.HijauApotekPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    transaction: Transaction,
    profile: PharmacyProfile,
    onDismiss: () -> Unit,
    onReplayVoice: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var paperWidth by remember { mutableStateOf(profile.printerPaperWidth) } // "58mm" or "80mm"
    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    fun formatRupiah(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    val receiptPlainText = remember(transaction, profile) {
        buildString {
            appendLine("================================")
            appendLine("       ${profile.pharmacyName}")
            appendLine("  ${profile.address}")
            appendLine("  Telp: ${profile.phone}")
            appendLine("  SIPA: ${profile.sipaNumber}")
            appendLine("  SIA: ${profile.siaNumber}")
            appendLine("  APA: ${profile.apotekerName}")
            appendLine("================================")
            appendLine("No. Faktur : ${transaction.invoiceNumber}")
            appendLine("Waktu      : ${dateFormatter.format(Date(transaction.timestamp))}")
            appendLine("Kasir      : ${transaction.cashierName}")
            appendLine("Pelanggan  : ${transaction.customerName}")
            if (transaction.isPrescription) {
                appendLine("Resep      : ${transaction.prescriptionNumber}")
                appendLine("Dokter     : ${transaction.doctorName}")
            }
            appendLine("--------------------------------")
            appendLine(transaction.itemsSummary)
            appendLine("--------------------------------")
            appendLine("Subtotal   : ${formatRupiah(transaction.subtotal)}")
            if (transaction.discountAmount > 0) {
                appendLine("Diskon     : -${formatRupiah(transaction.discountAmount)}")
            }
            appendLine("TOTAL      : ${formatRupiah(transaction.grandTotal)}")
            appendLine("Metode     : ${transaction.paymentMethod}")
            appendLine("Bayar      : ${formatRupiah(transaction.cashGiven)}")
            appendLine("Kembalian  : ${formatRupiah(transaction.changeAmount)}")
            appendLine("================================")
            appendLine("     Semoga Lekas Sembuh!")
            appendLine("  Obat yang dibeli tidak dapat")
            appendLine("        dikembalikan")
            appendLine("================================")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with success badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = HijauApotekPrimary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Transaksi Berhasil",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Voice Payment Proof Banner (Mengeluarkan kalimat harga yang diterima secara lisan)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FarmivaMintSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaPrimaryGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onReplayVoice?.invoke() }
                        .testTag("replay_payment_voice_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Bukti Suara",
                                tint = FarmivaForestGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Bukti Suara Pembayaran Lisan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmivaForestGreen
                                )
                                Text(
                                    text = "Mengeluarkan kalimat nominal yang diterima",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "🔊 Putar Ulang",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmivaPrimaryGreen
                        )
                    }
                }

                // Paper width toggle
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(text = "Format Struk: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(
                        selected = paperWidth == "58mm",
                        onClick = { paperWidth = "58mm" },
                        label = { Text("58mm (Kasir Kecil)", fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    FilterChip(
                        selected = paperWidth == "80mm",
                        onClick = { paperWidth = "80mm" },
                        label = { Text("80mm (Standar)", fontSize = 11.sp) }
                    )
                }

                // Thermal Paper Container (Authentic Monospace look)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(if (paperWidth == "58mm") 0.95f else 1f)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                    color = Color(0xFFFAFAFA),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.farmiva_custom_logo_1791555234480),
                            contentDescription = "Farmiva Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profile.pharmacyName,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = profile.address,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF333333),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Telp: ${profile.phone}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "SIPA: ${profile.sipaNumber} | SIA: ${profile.siaNumber}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "APA: ${profile.apotekerName} | TTK: ${profile.ttkName}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF555555)
                        )

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Meta details
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("No: ${transaction.invoiceNumber}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                                Text(dateFormatter.format(Date(transaction.timestamp)), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Black)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Kasir: ${transaction.cashierName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                                Text("Plg: ${transaction.customerName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                            }
                            if (transaction.isPrescription) {
                                Text("Dokter: ${transaction.doctorName} (Rx: ${transaction.prescriptionNumber})", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Blue)
                            }
                        }

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Itemized summary
                        Text(
                            text = transaction.itemsSummary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Financial totals
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                                Text(formatRupiah(transaction.subtotal), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                            }
                            if (transaction.discountAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Diskon (${transaction.discountPercent}%):", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Red)
                                    Text("-${formatRupiah(transaction.discountAmount)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Red)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                Text(formatRupiah(transaction.grandTotal), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Metode (${transaction.paymentMethod}):", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                                Text(formatRupiah(transaction.cashGiven), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kembalian:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Black)
                                Text(formatRupiah(transaction.changeAmount), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Black)
                            }
                        }

                        Text(
                            text = "==========================================",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "SEMOGA LEKAS SEMBUH!\nObat yang sudah dibeli tidak dapat ditukar/dikembalikan.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Struk Farmiva", receiptPlainText)
                            clipboard.setPrimaryClip(clip)

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, receiptPlainText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk Digital"))
                        },
                        modifier = Modifier.weight(1f).testTag("share_whatsapp_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.padding(end = 4.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Struk Kasir", receiptPlainText)
                            clipboard.setPrimaryClip(clip)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                        modifier = Modifier.weight(1f).testTag("print_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print", modifier = Modifier.padding(end = 4.dp))
                        Text("Cetak / Selesai", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
