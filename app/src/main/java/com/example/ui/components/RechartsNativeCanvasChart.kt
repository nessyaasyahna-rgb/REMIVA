package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.DailySalesMetric
import com.example.data.analytics.InventoryTurnoverMetric
import com.example.data.analytics.MonthlySalesMetric
import com.example.data.analytics.PharmacyAnalyticsEngine
import com.example.ui.theme.AlertWarningOrange
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.HijauApotekPrimary
import kotlin.math.max

@Composable
fun RechartsDailyAreaChart(
    dailyData: List<DailySalesMetric>,
    modifier: Modifier = Modifier
) {
    if (dailyData.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val maxVal = remember(dailyData) {
        max(dailyData.maxOf { it.salesAmount } * 1.15, 1_000_000.0)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dailyData) {
                    detectTapGestures(
                        onPress = { offset ->
                            val step = size.width / (dailyData.size - 1).coerceAtLeast(1)
                            val idx = ((offset.x / step).toInt()).coerceIn(0, dailyData.lastIndex)
                            selectedIndex = idx
                        }
                    )
                }
                .pointerInput(dailyData) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val step = size.width / (dailyData.size - 1).coerceAtLeast(1)
                            val idx = ((offset.x / step).toInt()).coerceIn(0, dailyData.lastIndex)
                            selectedIndex = idx
                        },
                        onDragEnd = {
                            // keep last selected or auto clear
                        },
                        onDrag = { change, _ ->
                            val step = size.width / (dailyData.size - 1).coerceAtLeast(1)
                            val idx = ((change.position.x / step).toInt()).coerceIn(0, dailyData.lastIndex)
                            selectedIndex = idx
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height - 24.dp.toPx() // bottom space for labels
            val stepX = w / (dailyData.size - 1).coerceAtLeast(1)
            val strokeDashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // 1. Grid Lines (Recharts CartesianGrid)
            val gridCount = 4
            for (i in 0..gridCount) {
                val y = (h / gridCount) * i
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = strokeDashed
                )
            }

            // 2. Build Smooth Area and Line Paths
            val points = dailyData.mapIndexed { idx, item ->
                val x = idx * stepX
                val y = h - (item.salesAmount / maxVal).toFloat() * h
                Offset(x, y)
            }

            val areaPath = Path().apply {
                moveTo(points.first().x, h)
                lineTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
                lineTo(points.last().x, h)
                close()
            }

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            // Draw Area Gradient (Recharts <Area fill="url(#colorSales)" />)
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HijauApotekPrimary.copy(alpha = 0.40f),
                        HijauApotekPrimary.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // Draw Curve Stroke
            drawPath(
                path = linePath,
                color = HijauApotekPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Data Dots
            points.forEachIndexed { idx, pt ->
                val isSel = idx == selectedIndex
                drawCircle(
                    color = if (isSel) Color.White else HijauApotekPrimary,
                    radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(),
                    center = pt
                )
                if (isSel) {
                    drawCircle(
                        color = HijauApotekPrimary,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    // Draw Recharts vertical cursor crosshair
                    drawLine(
                        color = HijauApotekPrimary.copy(alpha = 0.7f),
                        start = Offset(pt.x, 0f),
                        end = Offset(pt.x, h),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = strokeDashed
                    )
                }
            }
        }

        // X-Axis Date Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyData.forEachIndexed { idx, item ->
                Text(
                    text = item.dateLabel,
                    fontSize = 9.sp,
                    color = if (idx == selectedIndex) HijauApotekPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Floating Recharts Tooltip Overlay
        if (selectedIndex in dailyData.indices) {
            val item = dailyData[selectedIndex]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "${item.dateLabel} • ${item.txCount} Transaksi",
                        fontSize = 10.sp,
                        color = EmeraldGreenAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Omzet: ${PharmacyAnalyticsEngine.formatRp(item.salesAmount)}",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Laba Kotor: ${PharmacyAnalyticsEngine.formatRp(item.grossProfit)} (HPP: 70%)",
                        fontSize = 9.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

@Composable
fun RechartsMonthlyBarChart(
    monthlyData: List<MonthlySalesMetric>,
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val maxVal = remember(monthlyData) {
        max(monthlyData.maxOf { max(it.salesAmount, it.targetAmount) } * 1.15, 1_000_000.0)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(monthlyData) {
                    detectTapGestures { offset ->
                        val barStep = size.width / monthlyData.size
                        selectedIndex = (offset.x / barStep).toInt().coerceIn(0, monthlyData.lastIndex)
                    }
                }
        ) {
            val w = size.width
            val h = size.height - 24.dp.toPx()
            val barCount = monthlyData.size
            val stepX = w / barCount
            val barWidth = (stepX * 0.58f).coerceIn(8.dp.toPx(), 24.dp.toPx())
            val strokeDashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // Grid Lines
            val gridCount = 4
            for (i in 0..gridCount) {
                val y = (h / gridCount) * i
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = strokeDashed
                )
            }

            monthlyData.forEachIndexed { idx, item ->
                val centerX = idx * stepX + stepX / 2f
                val barHeight = (item.salesAmount / maxVal).toFloat() * h
                val targetHeight = (item.targetAmount / maxVal).toFloat() * h
                val topY = h - barHeight
                val targetY = h - targetHeight

                val isSelected = idx == selectedIndex
                val barColor = if (isSelected) EmeraldGreenAccent else HijauApotekPrimary

                // Rounded Top Bar (Recharts <Bar radius={[4, 4, 0, 0]} />)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Target Line marker (Recharts target benchmark)
                drawLine(
                    color = AlertWarningOrange,
                    start = Offset(centerX - barWidth * 0.7f, targetY),
                    end = Offset(centerX + barWidth * 0.7f, targetY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            monthlyData.forEachIndexed { idx, item ->
                Text(
                    text = item.monthLabel,
                    fontSize = 9.sp,
                    color = if (idx == selectedIndex) HijauApotekPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Floating Tooltip
        if (selectedIndex in monthlyData.indices) {
            val item = monthlyData[selectedIndex]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "${item.monthName} (Pencapaian ${String.format("%.1f", item.achievementPercent)}%)",
                        fontSize = 10.sp,
                        color = EmeraldGreenAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Omzet: ${PharmacyAnalyticsEngine.formatRp(item.salesAmount)}",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: ${PharmacyAnalyticsEngine.formatRp(item.targetAmount)} • Pertumbuhan: +${String.format("%.1f", item.growthMoM)}%",
                        fontSize = 9.sp,
                        color = AlertWarningOrange
                    )
                }
            }
        }
    }
}

@Composable
fun RechartsInventoryTurnoverChart(
    turnoverData: List<InventoryTurnoverMetric>,
    modifier: Modifier = Modifier
) {
    if (turnoverData.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val maxRatio = 10.0 // 0 to 10x per tahun

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(turnoverData) {
                    detectTapGestures { offset ->
                        val step = size.width / (turnoverData.size - 1).coerceAtLeast(1)
                        selectedIndex = (offset.x / step).toInt().coerceIn(0, turnoverData.lastIndex)
                    }
                }
        ) {
            val w = size.width
            val h = size.height - 24.dp.toPx()
            val stepX = w / (turnoverData.size - 1).coerceAtLeast(1)
            val strokeDashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // 1. Healthy Benchmark Band (4.0x - 8.0x per tahun)
            val yBandTop = h - (8.0 / maxRatio).toFloat() * h
            val yBandBottom = h - (4.0 / maxRatio).toFloat() * h
            drawRect(
                color = HijauApotekPrimary.copy(alpha = 0.09f),
                topLeft = Offset(0f, yBandTop),
                size = Size(w, yBandBottom - yBandTop)
            )

            // Grid lines
            for (i in 0..5) {
                val y = (h / 5) * i
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = strokeDashed
                )
            }

            // 2. Turnover Line & Area
            val points = turnoverData.mapIndexed { idx, item ->
                val x = idx * stepX
                val y = h - (item.turnoverRatio / maxRatio).toFloat() * h
                Offset(x, y)
            }

            val areaPath = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.35f),
                        Color(0xFF8B5CF6).copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.forEach { lineTo(it.x, it.y) }
            }

            drawPath(
                path = linePath,
                color = Color(0xFF8B5CF6),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Dots
            points.forEachIndexed { idx, pt ->
                val isSel = idx == selectedIndex
                drawCircle(
                    color = if (isSel) Color.White else Color(0xFF8B5CF6),
                    radius = if (isSel) 6.dp.toPx() else 4.dp.toPx(),
                    center = pt
                )
                if (isSel) {
                    drawCircle(
                        color = Color(0xFF8B5CF6),
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            turnoverData.forEachIndexed { idx, item ->
                Text(
                    text = "${item.monthLabel} (${String.format("%.1f", item.turnoverRatio)}x)",
                    fontSize = 9.sp,
                    color = if (idx == selectedIndex) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Tooltip
        if (selectedIndex in turnoverData.indices) {
            val item = turnoverData[selectedIndex]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "Periode ${item.monthLabel} 2026 • ${item.statusText}",
                        fontSize = 10.sp,
                        color = Color(0xFFA78BFA),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rasio ITR: ${String.format("%.1f", item.turnoverRatio)}x / tahun • DSI: ${item.daysSalesOfInventory} Hari",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rata-rata Nilai Stok: ${PharmacyAnalyticsEngine.formatRp(item.avgInventoryVal)}",
                        fontSize = 9.sp,
                        color = Color(0xFFD8B4FE)
                    )
                }
            }
        }
    }
}
