package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PharmacySalesAndTurnoverDashboard
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.AlertWarningOrange
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FarmivaViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: FarmivaViewModel) {
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val profile by viewModel.pharmacyProfile.collectAsState()

    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    fun formatRp(amount: Double): String = rupiahFormatter.format(amount).replace("Rp", "Rp ")

    // Calculations
    val totalRevenue = transactions.sumOf { it.grandTotal }
    val totalExpense = expenses.sumOf { it.amount }
    val netProfit = totalRevenue - totalExpense
    val todayTransactionsCount = transactions.size

    val lowStockCount = drugsWithBatches.count { it.isLowStock }
    // FEFO: items expiring within ~3 months (before 2027-02-01)
    val expiringCount = drugsWithBatches.count { drugItem ->
        drugItem.batches.any { it.expiryDate <= "2027-01-31" }
    }
    val fastMovingCount = drugsWithBatches.count { it.drug.isFastMoving }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(bottom = 96.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // Hero Banner Card with Apothecary interior visual
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.farmiva_hero_1791526777779),
                        contentDescription = "Apotek Farmiva",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xDD0F172A))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.farmiva_custom_logo_1791555234480),
                            contentDescription = "Farmiva Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = profile.pharmacyName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Commercial Pharmacy & FEFO Management System",
                                fontSize = 11.sp,
                                color = Color(0xFFD1FAE5)
                            )
                            Text(
                                text = "APA: ${profile.apotekerName} • SIPA: ${profile.sipaNumber}",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // Summary Cards Grid (Omzet, Keuntungan, Transaksi)
        item {
            Text(
                text = "Ringkasan Penjualan & Keuangan",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "Total Omzet",
                    value = formatRp(totalRevenue),
                    subtitle = "$todayTransactionsCount Transaksi Berhasil",
                    icon = Icons.Default.TrendingUp,
                    color = HijauApotekPrimary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Laba Bersih",
                    value = formatRp(netProfit),
                    subtitle = "Beban: ${formatRp(totalExpense)}",
                    icon = Icons.Default.Assessment,
                    color = EmeraldGreenAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stock Condition Gauges & FEFO Alerts
        item {
            Text(
                text = "Kondisi Stok & Peringatan FEFO",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StockAlertCard(
                    title = "Stok Menipis",
                    count = "$lowStockCount SKU",
                    description = "Di bawah batas minimum",
                    color = AlertWarningOrange,
                    onClick = { viewModel.navigateTo(AppScreen.STOK_FEFO) },
                    modifier = Modifier.weight(1f).testTag("alert_low_stock_card")
                )
                StockAlertCard(
                    title = "Mendekati Expired",
                    count = "$expiringCount Batch",
                    description = "Kadaluwarsa < 3 bulan",
                    color = AlertExpiringRed,
                    onClick = { viewModel.navigateTo(AppScreen.STOK_FEFO) },
                    modifier = Modifier.weight(1f).testTag("alert_expiring_fefo_card")
                )
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = "Akses Cepat Modul Apotek",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "Kasir (POS)",
                    icon = Icons.Default.PointOfSale,
                    color = HijauApotekPrimary,
                    onClick = { viewModel.navigateTo(AppScreen.POS_KASIR) },
                    modifier = Modifier.weight(1f).testTag("quick_action_pos")
                )
                QuickActionButton(
                    title = "Gudang & Mutasi",
                    icon = Icons.Default.Warehouse,
                    color = Color(0xFF0F766E),
                    onClick = { viewModel.navigateTo(AppScreen.GUDANG_MUTASI) },
                    modifier = Modifier.weight(1f).testTag("quick_action_warehouse")
                )
                QuickActionButton(
                    title = "Stock Opname",
                    icon = Icons.Default.Calculate,
                    color = Color(0xFF6366F1),
                    onClick = { viewModel.navigateTo(AppScreen.STOCK_OPNAME) },
                    modifier = Modifier.weight(1f).testTag("quick_action_opname")
                )
                QuickActionButton(
                    title = "SIPNAP Resep",
                    icon = Icons.Default.Description,
                    color = Color(0xFF8B5CF6),
                    onClick = { viewModel.navigateTo(AppScreen.SIPNAP_RESEP) },
                    modifier = Modifier.weight(1f).testTag("quick_action_sipnap")
                )
            }
        }

        // Recharts Interactive Pharmacy Sales & Inventory Turnover Dashboard
        item {
            PharmacySalesAndTurnoverDashboard(viewModel = viewModel)
        }

        // Recent Transactions Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Kasir Terbaru",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lihat Semua",
                    fontSize = 12.sp,
                    color = HijauApotekPrimary,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.RIWAYAT_TRANSAKSI) }
                )
            }
        }

        items(transactions.take(4)) { tx ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openReceiptModal(tx) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (tx.paymentMethod == "QRIS") Color(0xFF6366F1).copy(alpha = 0.15f) else HijauApotekPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = if (tx.paymentMethod == "QRIS") Color(0xFF6366F1) else HijauApotekPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.invoiceNumber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tx.customerName} • ${tx.paymentMethod}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatRp(tx.grandTotal),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.timestamp)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 10.sp, color = color)
        }
    }
}

@Composable
fun StockAlertCard(
    title: String,
    count: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
