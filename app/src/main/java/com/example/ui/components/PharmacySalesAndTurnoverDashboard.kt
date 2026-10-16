package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.analytics.CategoryTurnoverMetric
import com.example.data.analytics.PharmacyAnalyticsEngine
import com.example.ui.theme.AlertExpiringRed
import com.example.ui.theme.AlertWarningOrange
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.FarmivaViewModel

@Composable
fun PharmacySalesAndTurnoverDashboard(
    viewModel: FarmivaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val drugsWithBatches by viewModel.drugsWithBatches.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Daily, 1: Monthly, 2: Inventory Turnover, 3: Category Velocity
    var dailyDaysCount by remember { mutableIntStateOf(7) } // 7 or 30 days
    var isWebViewEngine by remember { mutableStateOf(false) }

    // Precompute analytics metrics reactive to database changes
    val dailyMetrics = remember(transactions, dailyDaysCount) {
        PharmacyAnalyticsEngine.computeDailyMetrics(transactions, dailyDaysCount)
    }
    val monthlyMetrics = remember(transactions) {
        PharmacyAnalyticsEngine.computeMonthlyMetrics(transactions)
    }
    val turnoverMetrics = remember(drugsWithBatches, monthlyMetrics) {
        PharmacyAnalyticsEngine.computeInventoryTurnoverMetrics(drugsWithBatches, monthlyMetrics)
    }
    val categoryTurnovers = remember(drugsWithBatches) {
        PharmacyAnalyticsEngine.computeCategoryTurnovers(drugsWithBatches)
    }
    val summary = remember(dailyMetrics, monthlyMetrics, turnoverMetrics, drugsWithBatches) {
        PharmacyAnalyticsEngine.computeSummary(dailyMetrics, monthlyMetrics, turnoverMetrics, drugsWithBatches)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag("pharmacy_analytics_dashboard_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HijauApotekPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = HijauApotekPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Analitik Kinerja & Recharts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = HijauApotekPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Recharts Engine",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HijauApotekPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Penjualan Harian/Bulanan & Perputaran Stok (ITR)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Share / Export icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val shareText = buildString {
                                appendLine("=== LAPORAN KINERJA APOTEK FARMIVA ===")
                                appendLine("Omzet Hari Ini : ${PharmacyAnalyticsEngine.formatRp(summary.todaySales)}")
                                appendLine("Bulan Berjalan : ${PharmacyAnalyticsEngine.formatRp(summary.monthToDateSales)} (${String.format("%.1f", summary.monthTargetAchievement)}% dari Target)")
                                appendLine("Rasio ITR      : ${String.format("%.1f", summary.currentTurnoverRatio)}x / tahun")
                                appendLine("Hari Stok (DSI): ${summary.currentDaysOfInventory} Hari (Sangat Sehat)")
                                appendLine("Total Modal Stok: ${PharmacyAnalyticsEngine.formatRp(summary.totalInventoryValuation)}")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Laporan Analitik Kinerja Apotek Farmiva")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan Analitik"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan Laporan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // KPI Summary Ribbons (4 Metrik Inti)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsKpiCard(
                    title = "Omzet Hari Ini",
                    value = PharmacyAnalyticsEngine.formatRp(summary.todaySales),
                    subtitle = if (summary.salesGrowthDaily >= 0) "+${String.format("%.1f", summary.salesGrowthDaily)}% vs Kemarin" else "${String.format("%.1f", summary.salesGrowthDaily)}%",
                    color = HijauApotekPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "Bulan Berjalan",
                    value = PharmacyAnalyticsEngine.formatRp(summary.monthToDateSales),
                    subtitle = "${String.format("%.1f", summary.monthTargetAchievement)}% Target",
                    color = EmeraldGreenAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsKpiCard(
                    title = "Perputaran Stok (ITR)",
                    value = "${String.format("%.1f", summary.currentTurnoverRatio)}x / thn",
                    subtitle = "Benchmark: 4x - 8x (Sehat)",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "Hari Stok (DSI)",
                    value = "${summary.currentDaysOfInventory} Hari",
                    subtitle = "Valuasi: ${PharmacyAnalyticsEngine.formatRp(summary.totalInventoryValuation)}",
                    color = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Engine Mode Switcher Bar (Native Compose Canvas vs Recharts WebView)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isWebViewEngine) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { isWebViewEngine = false }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = if (!isWebViewEngine) HijauApotekPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Native Canvas Recharts",
                        fontSize = 11.sp,
                        fontWeight = if (!isWebViewEngine) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isWebViewEngine) HijauApotekPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isWebViewEngine) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { isWebViewEngine = true }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Web,
                        contentDescription = null,
                        tint = if (isWebViewEngine) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recharts WebView Engine",
                        fontSize = 11.sp,
                        fontWeight = if (isWebViewEngine) FontWeight.Bold else FontWeight.Normal,
                        color = if (isWebViewEngine) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            val tabs = listOf(
                "Harian (Daily)" to Icons.Default.TrendingUp,
                "Bulanan (Monthly)" to Icons.Default.Assessment,
                "Turnover (ITR)" to Icons.Default.Timeline,
                "Kategori Stok" to Icons.Default.Inventory2
            )

            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = HijauApotekPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = HijauApotekPrimary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { idx, (label, icon) ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Tab Content
            if (isWebViewEngine) {
                // Render the interactive Recharts HTML5 Web component
                RechartsWebViewChart(
                    dailyData = dailyMetrics,
                    monthlyData = monthlyMetrics,
                    turnoverData = turnoverMetrics
                )
            } else {
                when (activeTab) {
                    0 -> {
                        // Daily Sales Tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tren Omzet Harian (${dailyDaysCount} Hari Terakhir)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = dailyDaysCount == 7,
                                    onClick = { dailyDaysCount = 7 },
                                    label = { Text("7 Hari", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HijauApotekPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = HijauApotekPrimary
                                    )
                                )
                                FilterChip(
                                    selected = dailyDaysCount == 30,
                                    onClick = { dailyDaysCount = 30 },
                                    label = { Text("30 Hari", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HijauApotekPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = HijauApotekPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Recharts Native Area Chart
                        RechartsDailyAreaChart(dailyData = dailyMetrics)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chart Legend & Insights
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(HijauApotekPrimary, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Omzet Penjualan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(EmeraldGreenAccent, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Margin Kotor (~30%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    1 -> {
                        // Monthly Sales Tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kinerja Penjualan Bulanan (Tahun 2026)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AlertWarningOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Target MoM +6%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertWarningOrange,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Recharts Monthly Bar Chart
                        RechartsMonthlyBarChart(monthlyData = monthlyMetrics)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Monthly Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(HijauApotekPrimary, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Realisasi Omzet (Jt)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp, 3.dp).background(AlertWarningOrange, RoundedCornerShape(1.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Garis Target (Jt)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    2 -> {
                        // Inventory Turnover (ITR) Tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tren Perputaran Persediaan (ITR & DSI)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Target: 4x - 8x / thn",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Recharts Turnover Chart
                        RechartsInventoryTurnoverChart(turnoverData = turnoverMetrics)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Turnover Explanatory Box
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Formula & Standar Kesehatan Apotek",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• ITR = HPP Terjual / Rata-rata Nilai Stok. Apotek Farmiva saat ini mencatat ITR ${String.format("%.1f", summary.currentTurnoverRatio)}x (Sangat Optimal).\n• DSI (Days Sales of Inventory) = ${summary.currentDaysOfInventory} Hari. Stok berputar cepat tanpa risiko kehabisan (stockout).",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    3 -> {
                        // Category Velocity Breakdown Tab
                        Text(
                            text = "Perputaran Berdasarkan Kategori Obat (FEFO Velocity)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categoryTurnovers.forEach { cat ->
                                CategoryVelocityRow(cat = cat)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actionable Pharmacy AI Intelligence Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HijauApotekPrimary.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, HijauApotekPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(HijauApotekPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = HijauApotekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rekomendasi Manajemen Stok Apoteker",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HijauApotekPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Perputaran kategori Obat Bebas & Flu sangat tinggi (10.4x). Disarankan menambah safety stock Paracetamol & OBH Combi sebesar 15% sebelum pergantian musim untuk mencegah kehilangan penjualan.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = color.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun CategoryVelocityRow(cat: CategoryTurnoverMetric) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cat.categoryName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Nilai Modal: ${PharmacyAnalyticsEngine.formatRp(cat.totalStockValue)} • ${cat.skuCount} SKU",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (cat.turnoverRatio >= 7.0) HijauApotekPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "${String.format("%.1f", cat.turnoverRatio)}x (${cat.daysInStock} Hari)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cat.turnoverRatio >= 7.0) HijauApotekPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cat.velocityRating,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
