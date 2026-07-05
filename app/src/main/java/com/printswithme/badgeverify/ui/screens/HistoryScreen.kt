package com.printswithme.badgeverify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.printswithme.badgeverify.Routes
import com.printswithme.badgeverify.data.model.HistoryItem
import com.printswithme.badgeverify.data.storage.HistoryStorage
import com.printswithme.badgeverify.ui.theme.*
import java.time.Duration
import java.time.Instant

private val DAY_MS = 24 * 60 * 60 * 1000L

data class CleanupOption(val id: String, val label: String, val description: String, val ms: Long?)

private val CLEANUP_OPTIONS = listOf(
    CleanupOption("24h", "Older than 24 hours", "Keep items from the last day", DAY_MS),
    CleanupOption("7d", "Older than 7 days", "Keep items from the last week", 7 * DAY_MS),
    CleanupOption("30d", "Older than 30 days", "Keep items from the last month", 30 * DAY_MS),
    CleanupOption("all", "Delete all history", "Clear every entry on this device", null),
)

@Composable
fun HistoryScreen(
    navController: NavController,
    historyStorage: HistoryStorage
) {
    var items by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCleanup by remember { mutableStateOf(false) }
    var pendingOption by remember { mutableStateOf<CleanupOption?>(null) }
    var openRowId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        items = historyStorage.getAll()
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Surface).statusBarsPadding()) {
        // ─── Header ───────────────────────────────────────────────────
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
            Text("History", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnSurface)
            if (items.isNotEmpty()) {
                IconButton(onClick = { showCleanup = !showCleanup; pendingOption = null }) {
                    Icon(
                        if (showCleanup) Icons.Filled.Close else Icons.Filled.FilterList,
                        contentDescription = "Cleanup",
                        tint = BrandPrimary
                    )
                }
            } else {
                Box(Modifier.size(40.dp))
            }
        }
        HorizontalDivider(color = Divider)

        // ─── Cleanup panel ────────────────────────────────────────────
        if (showCleanup) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSecondary)
                    .padding(16.dp)
            ) {
                Text("Delete verifications by age", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface)
                Spacer(Modifier.height(2.dp))
                Text("Pick a range to prune old entries. This cannot be undone.", fontSize = 13.sp, color = OnSurfaceSecondary)
                Spacer(Modifier.height(12.dp))
                CLEANUP_OPTIONS.forEach { opt ->
                    val isPending = pendingOption?.id == opt.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .clickable { pendingOption = opt }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isPending) BrandPrimary else Color.Transparent)
                                .then(
                                    if (!isPending) Modifier.background(
                                        Color.Transparent,
                                        CircleShape
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            RadioButton(
                                selected = isPending,
                                onClick = { pendingOption = opt },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                opt.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (opt.id == "all") Error else OnSurface
                            )
                            Text(opt.description, fontSize = 12.sp, color = OnSurfaceSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val opt = pendingOption ?: return@Button
                        if (opt.ms == null) {
                            items = historyStorage.clear()
                        } else {
                            items = historyStorage.deleteOlderThan(opt.ms)
                        }
                        pendingOption = null
                        showCleanup = false
                        openRowId = null
                    },
                    enabled = pendingOption != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pendingOption?.id == "all") Error else BrandPrimary,
                        disabledContainerColor = BorderStrong
                    )
                ) {
                    Text(
                        pendingOption?.let { "Apply · ${it.label}" } ?: "Select a range",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
            }
            HorizontalDivider(color = Divider)
        }

        // ─── List ─────────────────────────────────────────────────────
        if (items.isEmpty() && !loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(BrandTertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("No verifications yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface)
                Spacer(Modifier.height(8.dp))
                Text("Verified badges will appear here for quick re-access.", fontSize = 14.sp, color = OnSurfaceSecondary)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.verificationId }) { item ->
                    val isOpen = openRowId == item.verificationId
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceSecondary)
                                .clickable { navController.navigate(Routes.result(item.verificationId)) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandTertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (item.result.isPrivate) Icons.Filled.Lock else Icons.Filled.Shield,
                                    contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.result.companyName.ifEmpty { "Verified Badge" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnSurface, maxLines = 1)
                                Text(item.result.companyReason.ifEmpty { "Verification" }, fontSize = 13.sp, color = OnSurfaceSecondary, maxLines = 1)
                                Text(
                                    "${timeAgo(item.verifiedAt)} · ${item.verificationId.take(12)}${if (item.verificationId.length > 12) "…" else ""}",
                                    fontSize = 11.sp, color = OnSurfaceTertiary
                                )
                            }
                            IconButton(onClick = { openRowId = if (isOpen) null else item.verificationId }) {
                                Icon(
                                    if (isOpen) Icons.Filled.Close else Icons.Filled.MoreHoriz,
                                    contentDescription = null, tint = OnSurfaceSecondary
                                )
                            }
                        }
                        if (isOpen) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        items = historyStorage.delete(item.verificationId)
                                        openRowId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Remove this entry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun timeAgo(iso: String): String {
    return try {
        val diff = Duration.between(Instant.parse(iso), Instant.now())
        val m = diff.toMinutes()
        when {
            m < 1 -> "Just now"
            m < 60 -> "${m}m ago"
            m < 1440 -> "${m / 60}h ago"
            m < 10080 -> "${m / 1440}d ago"
            else -> java.time.LocalDate.ofInstant(Instant.parse(iso), java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (_: Exception) { "" }
}
