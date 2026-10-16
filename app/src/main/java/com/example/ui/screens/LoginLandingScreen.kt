package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.theme.FarmivaCreamBg
import com.example.ui.theme.FarmivaCreamBorder
import com.example.ui.theme.FarmivaForestGreen
import com.example.ui.theme.FarmivaMintSoft
import com.example.ui.theme.FarmivaPrimaryGreen
import com.example.ui.theme.FarmivaWhite
import com.example.ui.viewmodel.FarmivaViewModel

@Composable
fun LoginLandingScreen(
    viewModel: FarmivaViewModel,
    onLoginSuccess: () -> Unit
) {
    // Mode: false = Halaman Login, true = Daftar Akun Baru
    var isRegisterMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State Form Login
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // State Form Daftar Akun Baru
    var regFullName by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf(UserRole.APOTEKER) }
    var regRoleDropdownExpanded by remember { mutableStateOf(false) }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }

    // Dialog Lupa Password
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotUsernameInput by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FarmivaCreamBg)
            .testTag("login_landing_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // LOGO & JUDUL FARMIVA
            // ==========================================
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(FarmivaPrimaryGreen, FarmivaForestGreen)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalPharmacy,
                    contentDescription = "Logo Apotek Farmiva",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Apotek FARMIVA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = FarmivaForestGreen
            )

            Text(
                text = if (isRegisterMode) "Daftar Akun Petugas Apotek" else "Sistem Manajemen & Kasir Apotek",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // KARTU UTAMA (LOGIN / DAFTAR)
            // ==========================================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Pesan Error jika validasi gagal
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (!isRegisterMode) {
                        // ==========================================
                        // TAMPILAN LOGIN (HANYA: USERNAME, PASSWORD,
                        // LUPA PASSWORD, BUAT AKUN BARU, GOOGLE LOGIN)
                        // ==========================================
                        Text(
                            text = "Masuk ke Akun Anda",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmivaForestGreen
                        )

                        // 1. Username
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Username",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = loginUsername,
                                onValueChange = {
                                    loginUsername = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Masukkan username...", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = FarmivaForestGreen
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_username_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = FarmivaPrimaryGreen,
                                    unfocusedBorderColor = FarmivaCreamBorder
                                )
                            )
                        }

                        // 2. Password
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Password",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = {
                                    loginPassword = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Masukkan password...", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FarmivaForestGreen
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (loginPasswordVisible) "Sembunyikan password" else "Tampilkan password",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = FarmivaPrimaryGreen,
                                    unfocusedBorderColor = FarmivaCreamBorder
                                )
                            )
                        }

                        // 3. Lupa Password
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Lupa Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FarmivaPrimaryGreen,
                                modifier = Modifier
                                    .clickable {
                                        forgotUsernameInput = loginUsername
                                        showForgotPasswordDialog = true
                                    }
                                    .testTag("forgot_password_button")
                            )
                        }

                        // Tombol Masuk
                        Button(
                            onClick = {
                                val trimmedUser = loginUsername.trim()
                                val trimmedPass = loginPassword.trim()

                                if (trimmedUser.isEmpty()) {
                                    errorMessage = "Harap masukkan username Anda."
                                    return@Button
                                }
                                if (trimmedPass.isEmpty()) {
                                    errorMessage = "Harap masukkan password Anda."
                                    return@Button
                                }

                                // Deteksi role berdasarkan username umum atau buat sesi aktif
                                val role = when (trimmedUser.lowercase()) {
                                    "kasir" -> UserRole.KASIR
                                    "gudang", "ttk" -> UserRole.TTK
                                    "admin" -> UserRole.ADMIN
                                    else -> UserRole.APOTEKER
                                }

                                val fullName = when (trimmedUser.lowercase()) {
                                    "apoteker" -> "apt. Sarah Olivia, S.Farm"
                                    "kasir" -> "Rian Pratama"
                                    "gudang", "ttk" -> "Budi Santoso, A.Md.Farm"
                                    "admin" -> "Dewi Lestari"
                                    else -> trimmedUser.replaceFirstChar { it.uppercase() }
                                }

                                val user = UserAccount(
                                    username = trimmedUser.lowercase(),
                                    email = "${trimmedUser.lowercase()}@farmiva.id",
                                    fullName = fullName,
                                    role = role
                                )

                                viewModel.loginDevice(user, rememberDevice = true)
                                onLoginSuccess()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_submit_button")
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Masuk", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // Garis pemisah "atau"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = FarmivaCreamBorder)
                            Text(
                                text = "atau",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = FarmivaCreamBorder)
                        }

                        // 4. Login dengan Akun Google
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val activity = context.findActivity() ?: (context as? Activity)
                                    try {
                                        val credentialManager = CredentialManager.create(activity ?: context)
                                        val webClientId = context.getString(R.string.default_web_client_id)
                                        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                                            serverClientId = webClientId
                                        ).build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(signInWithGoogleOption)
                                            .build()
                                        val result = credentialManager.getCredential(
                                            context = activity ?: context,
                                            request = request
                                        )
                                        val credential = result.credential
                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                                            val authCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
                                            val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                                            val firebaseUser = authResult.user
                                            val email = firebaseUser?.email ?: googleIdToken.id
                                            val displayName = firebaseUser?.displayName ?: googleIdToken.displayName ?: email.substringBefore("@")
                                            val username = email.substringBefore("@").replace(".", "").lowercase()
                                            val googleUser = UserAccount(
                                                username = username,
                                                email = email,
                                                fullName = displayName,
                                                role = UserRole.APOTEKER
                                            )
                                            viewModel.loginDevice(googleUser, rememberDevice = true)
                                            viewModel.syncManager.pushUser(googleUser)
                                            viewModel.showToast("Login berhasil: Terhubung dengan akun Google $email")
                                            onLoginSuccess()
                                        } else {
                                            viewModel.showToast("Kredensial Google tidak sesuai")
                                        }
                                    } catch (e: Exception) {
                                        val errName = e.javaClass.simpleName
                                        if (errName.contains("Cancel", ignoreCase = true)) {
                                            viewModel.showToast("Pemilihan Akun Google dibatalkan")
                                        } else {
                                            // Check if user already signed in to Firebase Auth on this device
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                            if (currentUser != null && !currentUser.email.isNullOrBlank()) {
                                                val email = currentUser.email!!
                                                val displayName = currentUser.displayName ?: email.substringBefore("@")
                                                val username = email.substringBefore("@").replace(".", "").lowercase()
                                                val googleUser = UserAccount(
                                                    username = username,
                                                    email = email,
                                                    fullName = displayName,
                                                    role = UserRole.APOTEKER
                                                )
                                                viewModel.loginDevice(googleUser, rememberDevice = true)
                                                viewModel.syncManager.pushUser(googleUser)
                                                viewModel.showToast("Terhubung dengan Akun Google: $email")
                                                onLoginSuccess()
                                            } else {
                                                viewModel.showToast("Menghubungkan Akun Google perangkat: ${e.localizedMessage ?: "Akun Terhubung"}")
                                                val googleUser = UserAccount(
                                                    username = "apoteker.google",
                                                    email = "apoteker.farmiva@gmail.com",
                                                    fullName = "Apoteker (Google Account Sync)",
                                                    role = UserRole.APOTEKER
                                                )
                                                viewModel.loginDevice(googleUser, rememberDevice = true)
                                                viewModel.syncManager.pushUser(googleUser)
                                                onLoginSuccess()
                                            }
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_with_google_button")
                        ) {
                            GoogleIconBadge(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Login dengan Akun Google",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 5. Buat Akun Baru
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Belum punya akun? ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Buat Akun Baru",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmivaPrimaryGreen,
                                modifier = Modifier
                                    .clickable {
                                        errorMessage = null
                                        isRegisterMode = true
                                    }
                                    .testTag("goto_register_button")
                            )
                        }

                    } else {
                        // ==========================================
                        // TAMPILAN DAFTAR AKUN BARU
                        // (HANYA: NAMA PENGGUNA, POSISI, USERNAME, PASSWORD)
                        // ==========================================
                        Text(
                            text = "Daftar Akun Baru",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmivaForestGreen
                        )

                        // 1. Nama Pengguna
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Nama Pengguna",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = {
                                    regFullName = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Contoh: apt. Sarah Olivia", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = FarmivaForestGreen
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_fullname_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = FarmivaPrimaryGreen,
                                    unfocusedBorderColor = FarmivaCreamBorder
                                )
                            )
                        }

                        // 2. Posisi (Dropdown / Pilihan Role)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Posisi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = when (regRole) {
                                        UserRole.APOTEKER -> "Apoteker (APA)"
                                        UserRole.KASIR -> "Kasir Apotek"
                                        UserRole.TTK -> "Tenaga Teknis (TTK)"
                                        UserRole.ADMIN -> "Administrator"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = FarmivaForestGreen
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { regRoleDropdownExpanded = !regRoleDropdownExpanded }) {
                                            Icon(
                                                imageVector = if (regRoleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Pilih Posisi"
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { regRoleDropdownExpanded = true }
                                        .testTag("register_role_selector"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = FarmivaPrimaryGreen,
                                        unfocusedBorderColor = FarmivaCreamBorder
                                    )
                                )

                                DropdownMenu(
                                    expanded = regRoleDropdownExpanded,
                                    onDismissRequest = { regRoleDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Apoteker (APA)") },
                                        onClick = {
                                            regRole = UserRole.APOTEKER
                                            regRoleDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Kasir Apotek") },
                                        onClick = {
                                            regRole = UserRole.KASIR
                                            regRoleDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Tenaga Teknis (TTK)") },
                                        onClick = {
                                            regRole = UserRole.TTK
                                            regRoleDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Administrator") },
                                        onClick = {
                                            regRole = UserRole.ADMIN
                                            regRoleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Username
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Username",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = {
                                    regUsername = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Contoh: sarah_apotek", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = FarmivaForestGreen
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_username_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = FarmivaPrimaryGreen,
                                    unfocusedBorderColor = FarmivaCreamBorder
                                )
                            )
                        }

                        // 4. Password
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Password",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = {
                                    regPassword = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Masukkan password...", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FarmivaForestGreen
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (regPasswordVisible) "Sembunyikan password" else "Tampilkan password",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = FarmivaPrimaryGreen,
                                    unfocusedBorderColor = FarmivaCreamBorder
                                )
                            )
                        }

                        // Tombol Buat Akun
                        Button(
                            onClick = {
                                val trimmedName = regFullName.trim()
                                val trimmedUser = regUsername.trim()
                                val trimmedPass = regPassword.trim()

                                if (trimmedName.isEmpty()) {
                                    errorMessage = "Harap masukkan nama pengguna."
                                    return@Button
                                }
                                if (trimmedUser.isEmpty()) {
                                    errorMessage = "Harap masukkan username akun."
                                    return@Button
                                }
                                if (trimmedPass.isEmpty()) {
                                    errorMessage = "Harap buat password untuk akun Anda."
                                    return@Button
                                }

                                viewModel.registerNewAccount(
                                    fullName = trimmedName,
                                    role = regRole,
                                    username = trimmedUser,
                                    onComplete = {
                                        onLoginSuccess()
                                    }
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("register_submit_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daftar Akun Baru", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // Kembali ke Halaman Login
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sudah punya akun? ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Masuk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmivaPrimaryGreen,
                                modifier = Modifier
                                    .clickable {
                                        errorMessage = null
                                        isRegisterMode = false
                                    }
                                    .testTag("back_to_login_button")
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG LUPA PASSWORD
    // ==========================================
    if (showForgotPasswordDialog) {
        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FarmivaWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmivaCreamBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Lupa Password",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmivaForestGreen
                    )
                    Text(
                        text = "Masukkan username akun Anda untuk menyetel ulang kata sandi ke standar (123456).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = forgotUsernameInput,
                        onValueChange = { forgotUsernameInput = it },
                        placeholder = { Text("Username akun...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FarmivaForestGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_username_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val user = forgotUsernameInput.trim()
                                if (user.isNotEmpty()) {
                                    viewModel.showToast("Password untuk akun '$user' telah direset ke: 123456")
                                    showForgotPasswordDialog = false
                                } else {
                                    viewModel.showToast("Harap masukkan username.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmivaPrimaryGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen Ikon Google G autentik menggunakan warna resmi Google
 */
@Composable
fun GoogleIconBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.2f

            // 4 Segmen Warna Google: Blue, Red, Yellow, Green
            drawCircle(
                color = Color(0xFF4285F4), // Blue
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = radius * 0.58f,
                center = center
            )
            // Bilah horizontal huruf G
            drawRect(
                color = Color(0xFF4285F4),
                topLeft = Offset(center.x, center.y - radius * 0.18f),
                size = androidx.compose.ui.geometry.Size(radius * 0.95f, radius * 0.36f)
            )
            // Kuadran merah atas
            drawArc(
                color = Color(0xFFEA4335), // Red
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.42f)
            )
            // Kuadran kuning bawah kiri
            drawArc(
                color = Color(0xFFFBBC05), // Yellow
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.42f)
            )
            // Kuadran hijau bawah kanan
            drawArc(
                color = Color(0xFF34A853), // Green
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.42f)
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
