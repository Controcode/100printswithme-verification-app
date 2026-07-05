package com.printswithme.badgeverify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.printswithme.badgeverify.Routes
import com.printswithme.badgeverify.data.model.SecretKey
import com.printswithme.badgeverify.data.model.SecretsState
import com.printswithme.badgeverify.data.storage.SecretsStorage
import com.printswithme.badgeverify.ui.theme.*

@Composable
fun SecretsScreen(
    navController: NavController,
    secretsStorage: SecretsStorage
) {
    var secretsState by remember { mutableStateOf(SecretsState()) }
    var label by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    // Load state + consume any pending scanned secret
    LaunchedEffect(Unit) {
        secretsState = secretsStorage.getState()
        val pending = secretsStorage.consumePendingScannedSecret()
        if (pending != null) {
            secret = pending
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            // ─── Header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnSurface)
                }
                Text("Secret Key", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnSurface)
                Box(Modifier.size(40.dp))
            }
            HorizontalDivider(color = Divider)

            Spacer(Modifier.height(24.dp))

            // ─── Hero ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(BrandTertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Pair your private key", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = OnSurface, letterSpacing = (-0.3).sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Some badges require a secret key to verify. It is stored securely on this device and only used when verifying.",
                    fontSize = 14.sp, color = OnSurfaceSecondary, lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(24.dp))

            // ─── Input Fields ─────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("LABEL (optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnSurfaceSecondary, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("e.g. Production key", color = OnSurfaceTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Border,
                        unfocusedContainerColor = SurfaceSecondary,
                        focusedContainerColor = SurfaceSecondary
                    )
                )

                Spacer(Modifier.height(16.dp))
                Text("SECRET VALUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnSurfaceSecondary, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it; errorText = null },
                    placeholder = { Text("Paste or scan your secret key", color = OnSurfaceTertiary) },
                    singleLine = true,
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showSecret = !showSecret }) {
                            Icon(
                                if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle visibility", tint = OnSurfaceSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Border,
                        unfocusedContainerColor = SurfaceSecondary,
                        focusedContainerColor = SurfaceSecondary
                    )
                )

                if (errorText != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorText!!, fontSize = 13.sp, color = Error, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                // ─── Scan Key from QR ──────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSecondary)
                        .clickable { navController.navigate(Routes.scanner("secret")) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(BrandTertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Scan key from QR", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnSurface)
                            Text("Use your camera to import a secret", fontSize = 12.sp, color = OnSurfaceSecondary)
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OnSurfaceTertiary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Saved Keys ───────────────────────────────────────────
            if (secretsState.keys.isNotEmpty()) {
                HorizontalDivider(color = Divider)
                Spacer(Modifier.height(16.dp))
                Text(
                    "SAVED KEYS",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnSurfaceSecondary,
                    letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                secretsState.keys.forEach { key ->
                    val isActive = key.id == secretsState.activeKeyId
                    val isPendingDelete = pendingDeleteId == key.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceSecondary)
                            .clickable {
                                secretsState = secretsStorage.setActiveKey(
                                    if (isActive) null else key.id
                                )
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) BrandTertiary else SurfaceTertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = if (isActive) BrandPrimary else OnSurfaceTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(key.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnSurface)
                                    if (isActive) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(BrandPrimary)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                                Text("Added ${formatAddedAt(key.addedAt)}", fontSize = 12.sp, color = OnSurfaceTertiary)
                            }
                            // Delete button
                            IconButton(onClick = { pendingDeleteId = if (isPendingDelete) null else key.id }) {
                                Icon(
                                    if (isPendingDelete) Icons.Filled.Close else Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = if (isPendingDelete) OnSurfaceSecondary else OnSurfaceTertiary
                                )
                            }
                        }
                        if (isPendingDelete) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { pendingDeleteId = null },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
                                ) {
                                    Text("Cancel", fontSize = 13.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        secretsState = secretsStorage.deleteKey(key.id)
                                        pendingDeleteId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Remove Key", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(100.dp))
        }

        // ─── Sticky Save Button ────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    val v = secret.trim()
                    if (v.isEmpty()) {
                        errorText = "Secret value cannot be empty."
                        return@Button
                    }
                    try {
                        secretsState = secretsStorage.addKey(label, v)
                        label = ""
                        secret = ""
                        errorText = null
                    } catch (e: Exception) {
                        errorText = e.message ?: "Failed to save key."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save Key", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

private fun formatAddedAt(iso: String): String {
    return try {
        val dt = java.time.LocalDate.ofInstant(
            java.time.Instant.parse(iso), java.time.ZoneId.systemDefault()
        )
        dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: Exception) { iso }
}
