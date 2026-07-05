package com.printswithme.badgeverify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.printswithme.badgeverify.Routes
import com.printswithme.badgeverify.data.api.BadgeApi
import com.printswithme.badgeverify.data.api.VerifyResult
import com.printswithme.badgeverify.data.api.verifyBadgeSafe
import com.printswithme.badgeverify.data.model.HistoryItem
import com.printswithme.badgeverify.data.storage.HistoryStorage
import com.printswithme.badgeverify.data.storage.SecretsStorage
import com.printswithme.badgeverify.ui.theme.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant

private fun timeAgo(iso: String): String {
    return try {
        val diff = Duration.between(Instant.parse(iso), Instant.now())
        val m = diff.toMinutes()
        when {
            m < 1 -> "Just now"
            m < 60 -> "${m}m ago"
            m < 1440 -> "${m / 60}h ago"
            m < 10080 -> "${m / 1440}d ago"
            else -> java.time.LocalDateTime.ofInstant(
                Instant.parse(iso), java.time.ZoneId.systemDefault()
            ).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (_: Exception) { "" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    historyStorage: HistoryStorage,
    secretsStorage: SecretsStorage,
    api: BadgeApi
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var history by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var activeKeyLabel by remember { mutableStateOf<String?>(null) }
    var manualId by remember { mutableStateOf("") }
    var verifying by remember { mutableStateOf(false) }

    // Load history and active key whenever screen is composed/resumed
    LaunchedEffect(Unit) {
        history = historyStorage.getAll()
        val state = secretsStorage.getState()
        activeKeyLabel = state.keys.find { it.id == state.activeKeyId }?.label
    }

    fun runManualVerify() {
        val id = manualId.trim()
        if (id.isEmpty() || verifying) return
        focusManager.clearFocus()
        scope.launch {
            verifying = true
            try {
                val secret = secretsStorage.getActiveSecretValue().ifEmpty { null }
                val result = api.verifyBadgeSafe(id, secret)
                when (result) {
                    is VerifyResult.Success -> {
                        historyStorage.add(HistoryItem(
                            verificationId = result.data.id.ifEmpty { id },
                            verifiedAt = Instant.now().toString(),
                            result = result.data
                        ))
                        manualId = ""
                        history = historyStorage.getAll()
                        navController.navigate(Routes.result(result.data.id.ifEmpty { id }))
                    }
                    is VerifyResult.Failure -> {
                        navController.navigate(
                            Routes.resultError(id, result.message, result.needsSecret)
                        )
                    }
                }
            } finally {
                verifying = false
            }
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandTertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("100PrintsWithMe", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = OnSurface, letterSpacing = (-0.3).sp)
                        Text("Badge Verifier", fontSize = 12.sp, color = OnSurfaceSecondary)
                    }
                }
                IconButton(
                    onClick = { navController.navigate(Routes.SECRETS) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceSecondary)
                ) {
                    Icon(
                        if (activeKeyLabel != null) Icons.Filled.Key else Icons.Outlined.Key,
                        contentDescription = "Secret keys",
                        tint = if (activeKeyLabel != null) BrandPrimary else OnSurfaceSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ─── Hero Card ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
                    .padding(24.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Verify a Badge", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.White, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan the QR on any 100Prints badge or certificate to instantly confirm authenticity.",
                        fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Routes.scanner()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, tint = BrandPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan QR Code", color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    if (activeKeyLabel != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(activeKeyLabel!!, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Manual ID Input ──────────────────────────────────────
            Text(
                "OR ENTER VERIFICATION ID",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = OnSurfaceSecondary, letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualId,
                    onValueChange = { manualId = it },
                    placeholder = { Text("e.g. 8f1c-9b2a-…", color = OnSurfaceTertiary) },
                    leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null, tint = OnSurfaceTertiary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Border,
                        unfocusedContainerColor = SurfaceSecondary,
                        focusedContainerColor = SurfaceSecondary
                    )
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (manualId.trim().isNotEmpty() && !verifying) BrandPrimary else SurfaceTertiary)
                        .clickable(enabled = manualId.trim().isNotEmpty() && !verifying) { runManualVerify() },
                    contentAlignment = Alignment.Center
                ) {
                    if (verifying) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.ArrowForward, contentDescription = "Verify", tint = if (manualId.trim().isNotEmpty()) Color.White else OnSurfaceTertiary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Recent Verifications ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT VERIFICATIONS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnSurfaceSecondary, letterSpacing = 1.2.sp)
                if (history.isNotEmpty()) {
                    TextButton(onClick = { navController.navigate(Routes.HISTORY) }) {
                        Text("${history.size}", color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (history.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceSecondary)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = OnSurfaceTertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("No verifications yet. Scan or enter a badge to get started.", fontSize = 13.sp, color = OnSurfaceSecondary)
                }
            } else {
                history.take(5).forEachIndexed { idx, item ->
                    if (idx > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Routes.result(item.verificationId)) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Success)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.result.companyName.ifEmpty { "Verified Badge" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnSurface, maxLines = 1)
                            Text(item.result.companyReason.ifEmpty { "Verification" }, fontSize = 13.sp, color = OnSurfaceSecondary, maxLines = 1)
                        }
                        Text(timeAgo(item.verifiedAt), fontSize = 13.sp, color = OnSurfaceTertiary)
                    }
                }
                if (history.size > 5) {
                    TextButton(
                        onClick = { navController.navigate(Routes.HISTORY) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("See all history", color = BrandPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // ─── Footer ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface)
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Powered by 100PrintsWithMe", fontSize = 12.sp, color = OnSurfaceTertiary, fontWeight = FontWeight.Medium)
        }

        // ─── Verifying overlay ────────────────────────────────────────
        if (verifying) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x590F172A)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("Verifying…", fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                }
            }
        }
    }
}
