package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AuthSwitcherDialog
import com.example.ui.components.FarmivaTopBar
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinancialReportScreen
import com.example.ui.screens.InventoryFefoScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SettingsAndProfileScreen
import com.example.ui.screens.SipnapPrescriptionScreen
import com.example.ui.screens.StockOpnameScreen
import com.example.ui.screens.WarehouseScreen
import com.example.ui.screens.LoginLandingScreen
import com.example.ui.screens.PastTransactionsScreen
import com.example.ui.theme.FarmivaTheme
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FarmivaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FarmivaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isLoggedInDevice by viewModel.isLoggedInDevice.collectAsState()

            FarmivaTheme(darkTheme = isDarkMode) {
                if (!isLoggedInDevice) {
                    LoginLandingScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            // Sesi tersimpan, langsung masuk ke aplikasi
                        }
                    )
                } else {
                    FarmivaApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FarmivaApp(viewModel: FarmivaViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val lastCompletedTx by viewModel.lastCompletedTransaction.collectAsState()
    val showReceipt by viewModel.showReceiptModal.collectAsState()
    val profile by viewModel.pharmacyProfile.collectAsState()

    var showAuthModal by remember { mutableStateOf(false) }

    // Back handling: pop to Dashboard if on a sub-screen
    if (currentScreen != AppScreen.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(AppScreen.DASHBOARD)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            FarmivaTopBar(
                viewModel = viewModel,
                onOpenAuthModal = { showAuthModal = true }
            )
        },
        bottomBar = {
            FarmivaBottomNavBar(
                currentScreen = currentScreen,
                onSelectScreen = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.POS_KASIR -> PosScreen(viewModel = viewModel)
                AppScreen.STOK_FEFO -> InventoryFefoScreen(viewModel = viewModel)
                AppScreen.GUDANG_MUTASI -> WarehouseScreen(viewModel = viewModel)
                AppScreen.STOCK_OPNAME -> StockOpnameScreen(viewModel = viewModel)
                AppScreen.SIPNAP_RESEP -> SipnapPrescriptionScreen(viewModel = viewModel)
                AppScreen.KEUANGAN_LAPORAN -> FinancialReportScreen(viewModel = viewModel)
                AppScreen.PENGATURAN_BACKUP -> SettingsAndProfileScreen(viewModel = viewModel)
                AppScreen.RIWAYAT_TRANSAKSI -> PastTransactionsScreen(viewModel = viewModel)
            }

            // In-app Floating Toast / Banner
            AnimatedVisibility(
                visible = toastMsg != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                toastMsg?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    // Thermal Receipt Dialog
    if (showReceipt && lastCompletedTx != null) {
        ReceiptDialog(
            transaction = lastCompletedTx!!,
            profile = profile,
            onDismiss = { viewModel.dismissReceiptModal() },
            onReplayVoice = { viewModel.speakPaymentReceipt(lastCompletedTx!!.grandTotal) }
        )
    }

    // Auth Switcher & Account Dialog
    if (showAuthModal) {
        AuthSwitcherDialog(
            viewModel = viewModel,
            onDismiss = { showAuthModal = false }
        )
    }
}

@Composable
fun FarmivaBottomNavBar(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    // 5 primary core icons on bottom bar, with POS highlighted in center
    val navItems = listOf(
        Pair(AppScreen.DASHBOARD, Icons.Default.Dashboard),
        Pair(AppScreen.STOK_FEFO, Icons.Default.Inventory2),
        Pair(AppScreen.POS_KASIR, Icons.Default.PointOfSale),
        Pair(AppScreen.GUDANG_MUTASI, Icons.Default.Warehouse),
        Pair(AppScreen.PENGATURAN_BACKUP, Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        navItems.forEach { (screen, icon) ->
            val isSelected = currentScreen == screen
            val isPos = screen == AppScreen.POS_KASIR

            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = screen.title,
                        tint = if (isSelected) HijauApotekPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = if (isPos) "KASIR" else screen.title.split(" ").first(),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected || isPos) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) HijauApotekPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (isPos) HijauApotekPrimary.copy(alpha = 0.2f) else HijauApotekPrimary.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
            )
        }
    }
}
