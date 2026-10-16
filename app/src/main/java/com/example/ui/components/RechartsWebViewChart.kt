package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.analytics.DailySalesMetric
import com.example.data.analytics.InventoryTurnoverMetric
import com.example.data.analytics.MonthlySalesMetric
import com.example.data.analytics.PharmacyAnalyticsEngine

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWebViewChart(
    dailyData: List<DailySalesMetric>,
    monthlyData: List<MonthlySalesMetric>,
    turnoverData: List<InventoryTurnoverMetric>,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(dailyData, monthlyData, turnoverData) {
        PharmacyAnalyticsEngine.buildRechartsHtml(dailyData, monthlyData, turnoverData)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    webViewClient = WebViewClient()
                    setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxWidth().height(340.dp)
        )
    }
}
