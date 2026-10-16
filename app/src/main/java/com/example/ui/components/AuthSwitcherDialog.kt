package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.theme.HijauApotekPrimary
import com.example.ui.viewmodel.FarmivaViewModel

@Composable
fun AuthSwitcherDialog(
    viewModel: FarmivaViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isGuest = currentUser.username == "guest"

    var selectedTab by remember { mutableStateOf(if (isGuest) 0 else 0) } // 0: Login, 1: Register, 2: Reset Password
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Register fields
    var regFullName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf(UserRole.KASIR) }

    // Reset password field
    var resetEmail by remember { mutableStateOf("") }
    var resetSentMessage by remember { mutableStateOf<String?>(null) }

    // Logout Confirmation state
    var showLogoutConfirm by remember { mutableStateOf(false) }

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
                                .background(HijauApotekPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = HijauApotekPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Akun & Keamanan Sesi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kelola pengguna, hak akses & sesi login",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ACTIVE USER SESSION CARD WITH LOGOUT BUTTON
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isGuest) HijauApotekPrimary.copy(alpha = 0.08f) else Color(0xFFF1F5F9)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!isGuest) HijauApotekPrimary.copy(alpha = 0.25f) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                        .background(if (!isGuest) HijauApotekPrimary else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (!isGuest) currentUser.fullName.take(1).uppercase() else "?",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentUser.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (!isGuest) "${currentUser.role.roleTitle}  •  ${currentUser.email.ifBlank { "Aktif" }}" else "Belum Login (Akses Terbatas)",
                                        fontSize = 10.sp,
                                        color = if (!isGuest) HijauApotekPrimary else Color.Gray
                                    )
                                }
                            }

                            // Status Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isGuest) Color(0xFF16A34A).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (!isGuest) Color(0xFF16A34A) else Color(0xFF64748B))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (!isGuest) "Online" else "Guest",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isGuest) Color(0xFF16A34A) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        if (!isGuest) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = HijauApotekPrimary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // LOGOUT BUTTON
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ingin keluar sesi?",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { showLogoutConfirm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("logout_account_button")
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Logout / Keluar Akun", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Switch Role presets
                Text(
                    text = "Ganti Akun Cepat (Preset Role Apotek):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Pair("Apoteker", UserAccount(username = "apoteker", email = "sarah.apa@farmiva.id", fullName = "apt. Sarah Olivia, S.Farm", role = UserRole.APOTEKER)),
                        Pair("Admin", UserAccount(username = "admin", email = "admin@farmiva.id", fullName = "H. Hendra Pratama, S.E.", role = UserRole.ADMIN)),
                        Pair("Kasir", UserAccount(username = "kasir", email = "rina@farmiva.id", fullName = "Rina Safitri", role = UserRole.KASIR)),
                        Pair("TTK", UserAccount(username = "ttk", email = "budi@farmiva.id", fullName = "Budi Santoso, A.Md.Farm", role = UserRole.TTK))
                    )

                    presets.forEach { (label, account) ->
                        val isSelected = currentUser.username == account.username
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.switchUser(account)
                                viewModel.showToast("Beralih ke akun ${account.fullName} (${account.role.roleTitle})")
                                onDismiss()
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Login, Register, Lupa Password
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Login Form", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Daftar Baru", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Lupa Sandi", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> { // LOGIN
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = { usernameOrEmail = it },
                                label = { Text("Username atau Email") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("login_username_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Kata Sandi") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val match = if (usernameOrEmail.contains("admin", ignoreCase = true)) {
                                        UserAccount(username = "admin", email = "admin@farmiva.id", fullName = "H. Hendra Pratama, S.E.", role = UserRole.ADMIN)
                                    } else if (usernameOrEmail.contains("kasir", ignoreCase = true)) {
                                        UserAccount(username = "kasir", email = "rina@farmiva.id", fullName = "Rina Safitri", role = UserRole.KASIR)
                                    } else if (usernameOrEmail.contains("ttk", ignoreCase = true)) {
                                        UserAccount(username = "ttk", email = "budi@farmiva.id", fullName = "Budi Santoso, A.Md.Farm", role = UserRole.TTK)
                                    } else {
                                        UserAccount(username = "apoteker", email = "sarah.apa@farmiva.id", fullName = "apt. Sarah Olivia, S.Farm", role = UserRole.APOTEKER)
                                    }
                                    viewModel.switchUser(match)
                                    viewModel.showToast("Berhasil login sebagai ${match.fullName}")
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("login_submit_button")
                            ) {
                                Text("Masuk ke Sistem", fontWeight = FontWeight.Bold)
                            }

                            // Google Sign-In Button
                            OutlinedButton(
                                onClick = {
                                    val googleUser = UserAccount(
                                        username = "google_user",
                                        email = "apoteker.google@farmiva.id",
                                        fullName = "apt. Google Certified Apoteker",
                                        role = UserRole.APOTEKER
                                    )
                                    viewModel.switchUser(googleUser)
                                    viewModel.showToast("Berhasil Masuk via Google OAuth2!")
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button")
                            ) {
                                Text("Masuk dengan Akun Google", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    1 -> { // REGISTER
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = { regFullName = it },
                                label = { Text("Nama Lengkap & Gelar") },
                                modifier = Modifier.fillMaxWidth().testTag("reg_fullname_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = { regUsername = it },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth().testTag("reg_username_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                label = { Text("Email Perusahaan/Apotek") },
                                modifier = Modifier.fillMaxWidth().testTag("reg_email_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it },
                                label = { Text("No. WhatsApp") },
                                modifier = Modifier.fillMaxWidth().testTag("reg_phone_input"),
                                singleLine = true
                            )

                            Text(
                                text = "Pilih Hak Akses (Role):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                UserRole.values().forEach { role ->
                                    FilterChip(
                                        selected = regRole == role,
                                        onClick = { regRole = role },
                                        label = { Text(role.name, fontSize = 10.sp) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val newUser = UserAccount(
                                        username = regUsername.ifBlank { "user_${System.currentTimeMillis() % 1000}" },
                                        email = regEmail.ifBlank { "user@farmiva.id" },
                                        fullName = regFullName.ifBlank { "Petugas Apotek" },
                                        role = regRole,
                                        phone = regPhone
                                    )
                                    viewModel.switchUser(newUser)
                                    viewModel.showToast("Pendaftaran Berhasil! Login sebagai ${newUser.fullName}")
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("register_submit_button")
                            ) {
                                Text("Daftarkan Akun & Beri Hak Akses", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    2 -> { // RESET PASSWORD
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Masukkan email akun Anda. Sistem akan mengirimkan link reset password serta kode OTP verifikasi ke email terdaftar via SMTP.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                label = { Text("Email Terdaftar") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("reset_email_input"),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    resetSentMessage = "Link reset password & OTP 6-digit berhasil dikirimkan ke $resetEmail (simulasi SMTP/SendGrid)."
                                    viewModel.showToast("Email reset sandi telah dikirim!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HijauApotekPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("send_reset_link_button")
                            ) {
                                Text("Kirim Link & OTP Reset Sandi")
                            }

                            resetSentMessage?.let { msg ->
                                Surface(
                                    color = HijauApotekPrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        fontSize = 11.sp,
                                        color = HijauApotekPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CONFIRM LOGOUT DIALOG
    if (showLogoutConfirm) {
        Dialog(onDismissRequest = { showLogoutConfirm = false }) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Konfirmasi Logout Akun",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Apakah Anda yakin ingin keluar dari akun ${currentUser.fullName} (${currentUser.role.roleTitle})?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sesi aktif akan diakhiri. Anda dapat masuk kembali kapan saja dengan memasukkan username atau menggunakan preset akun.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showLogoutConfirm = false }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showLogoutConfirm = false
                                viewModel.logout()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("confirm_logout_action_button")
                        ) {
                            Text("Ya, Keluar Akun")
                        }
                    }
                }
            }
        }
    }
}
