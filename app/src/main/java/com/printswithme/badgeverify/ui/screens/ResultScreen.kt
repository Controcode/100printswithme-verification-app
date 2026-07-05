package com.printswithme.badgeverify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.printswithme.badgeverify.Routes
import com.printswithme.badgeverify.data.api.BadgeApi
import com.printswithme.badgeverify.data.api.VerifyResult
import com.printswithme.badgeverify.data.api.verifyBadgeSafe
import com.printswithme.badgeverify.data.model.HistoryItem
import com.printswithme.badgeverify.data.model.VerificationResult
import com.printswithme.badgeverify.data.storage.HistoryStorage
import com.printswithme.badgeverify.data.storage.SecretsStorage
import com.printswithme.badgeverify.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun formatDate(iso: String?): String {
    if (iso.isNullOrEmpty()) return "—"
    return try {
        val dt = LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy, hh:mm a"))
    } catch (_: Exception) { iso }
}

private fun humanizeKey(key: String): String =
    key.replace(Regex("[_-]+"), " ")
        .replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
        .trim()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

private fun renderValue(value: Any?): String = when (value) {
    null -> "—"
    is String -> value
    is Number, is Boolean -> value.toString()
    else -> value.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController,
    verificationId: String,
    initialError: String?,
    initialNeedsSecret: Boolean,
    historyStorage: HistoryStorage,
    secretsStorage: SecretsStorage,
    api: BadgeApi
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(initialError == null) }
    var data by remember { mutableStateOf<VerificationResult?>(null) }
    var error by remember { mutableStateOf(initialError) }
    var needsSecret by remember { mutableStateOf(initialNeedsSecret) }

    LaunchedEffect(verificationId) {
        if (initialError != null) return@LaunchedEffect
        // Check local history cache first
        val cached = historyStorage.getAll().find { it.verificationId == verificationId }
        if (cached != null) {
            data = cached.result
            loading = false
            return@LaunchedEffect
        }
        // Fetch from API
        val secret = secretsStorage.getActiveSecretValue().ifEmpty { null }
        when (val result = api.verifyBadgeSafe(verificationId, secret)) {
            is VerifyResult.Success -> {
                data = result.data
                historyStorage.add(HistoryItem(
                    verificationId = result.data.id.ifEmpty { verificationId },
                    verifiedAt = Instant.now().toString(),
                    result = result.data
                ))
            }
            is VerifyResult.Failure -> {
                error = result.message
                needsSecret = result.needsSecret
            }
        }
        loading = false
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = OnSurface, modifier = Modifier.size(26.dp))
                    }
                    Text("Verification", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnSurface)
                    Box(Modifier.size(32.dp))
                }
                HorizontalDivider(color = Divider)
            }
        },
        containerColor = Surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("Verifying badge…", color = OnSurfaceSecondary)
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(88.dp).clip(CircleShape).background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = Error, modifier = Modifier.size(56.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Verification Failed", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, fontSize = 14.sp, color = OnSurfaceSecondary, lineHeight = 20.sp)
                        if (needsSecret) {
                            Spacer(Modifier.height(8.dp))
                            Text("Tap the key icon on the home screen to add your secret.", fontSize = 13.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate(Routes.scanner()) { popUpTo(Routes.RESULT) { inclusive = true } } },
                            colors = ButtonDefaults.buttonColors(containerColor = Error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Scan Another", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                data != null -> {
                    val d = data!!
                    val publicEntries = d.publicData?.entries?.toList() ?: emptyList()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // ─── Success Hero ──────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))))
                                .padding(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("VERIFIED", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(d.companyName, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White, maxLines = 2)
                                Spacer(Modifier.height(4.dp))
                                Text(d.companyReason, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
                                if (d.isPrivate) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Private Badge", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), letterSpacing = 0.5.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // ─── Meta Card ─────────────────────────────────
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Issued", fontSize = 13.sp, color = OnSurfaceSecondary, fontWeight = FontWeight.SemiBold)
                                    Text(formatDate(d.createdAt), fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = Divider)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Verification ID", fontSize = 13.sp, color = OnSurfaceSecondary, fontWeight = FontWeight.SemiBold)
                                    Text(d.id, fontSize = 13.sp, color = OnSurface, fontFamily = FontFamily.Monospace, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // ─── Public Data ───────────────────────────────
                        if (publicEntries.isNotEmpty()) {
                            Text("BADGE DETAILS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnSurfaceSecondary, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            Spacer(Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    publicEntries.forEachIndexed { idx, (k, v) ->
                                        val valueStr = renderValue(v)
                                        val isLong = valueStr.length > 80 || valueStr.contains("\n")
                                        if (isLong) {
                                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                                Text(humanizeKey(k), fontSize = 13.sp, color = OnSurfaceSecondary, fontWeight = FontWeight.SemiBold)
                                                Spacer(Modifier.height(4.dp))
                                                Text(valueStr, fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.Medium)
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(humanizeKey(k), fontSize = 13.sp, color = OnSurfaceSecondary, fontWeight = FontWeight.SemiBold)
                                                Text(valueStr, fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                        if (idx < publicEntries.lastIndex) HorizontalDivider(color = Divider)
                                    }
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("No additional badge data.", fontSize = 13.sp, color = OnSurfaceSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // ─── Scan Another ──────────────────────────────
                        Button(
                            onClick = { navController.navigate(Routes.scanner()) { popUpTo(Routes.RESULT) { inclusive = true } } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Scan Another", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}
