package com.printswithme.badgeverify.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.printswithme.badgeverify.Routes
import com.printswithme.badgeverify.camera.BarcodeAnalyzer
import com.printswithme.badgeverify.data.api.BadgeApi
import com.printswithme.badgeverify.data.api.VerifyResult
import com.printswithme.badgeverify.data.api.verifyBadgeSafe
import com.printswithme.badgeverify.data.model.HistoryItem
import com.printswithme.badgeverify.data.storage.HistoryStorage
import com.printswithme.badgeverify.data.storage.SecretsStorage
import com.printswithme.badgeverify.ui.components.ViewfinderOverlay
import com.printswithme.badgeverify.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    navController: NavController,
    mode: String,
    historyStorage: HistoryStorage,
    secretsStorage: SecretsStorage,
    api: BadgeApi
) {
    val isSecretMode = mode == "secret"
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var torchEnabled by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var activeKeyLabel by remember { mutableStateOf<String?>(null) }

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeAnalyzer = remember {
        BarcodeAnalyzer { data ->
            if (verifying) return@BarcodeAnalyzer
            verifying = true
            scope.launch {
                if (isSecretMode) {
                    var value = data.trim()
                    try {
                        if (value.startsWith("http")) {
                            val uri = android.net.Uri.parse(value)
                            uri.getQueryParameter("secret")?.let { value = it }
                        }
                    } catch (_: Exception) {}
                    secretsStorage.setPendingScannedSecret(value)
                    navController.popBackStack()
                } else {
                    val secret = secretsStorage.getActiveSecretValue().ifEmpty { null }
                    val result = api.verifyBadgeSafe(data, secret)
                    when (result) {
                        is VerifyResult.Success -> {
                            historyStorage.add(HistoryItem(
                                verificationId = result.data.id.ifEmpty { data },
                                verifiedAt = Instant.now().toString(),
                                result = result.data
                            ))
                            navController.navigate(Routes.result(result.data.id.ifEmpty { data })) {
                                popUpTo(Routes.scanner()) { inclusive = true }
                            }
                        }
                        is VerifyResult.Failure -> {
                            navController.navigate(Routes.resultError(data, result.message, result.needsSecret)) {
                                popUpTo(Routes.scanner()) { inclusive = true }
                            }
                        }
                    }
                    delay(1500)
                }
                verifying = false
            }
        }
    }

    // Highly optimized scanning line animation using graphicsLayer to avoid recomposition
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val translateY by infiniteTransition.animateFloat(
        initialValue = -110f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translateY"
    )

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
        val state = secretsStorage.getState()
        activeKeyLabel = state.keys.find { it.id == state.activeKeyId }?.label
    }

    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeAnalyzer.close()
        }
    }

    if (!cameraPermission.status.isGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface)
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(BrandTertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Camera, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                if (isSecretMode) "Scan secret key" else "Camera access needed",
                fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = OnSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (cameraPermission.status.shouldShowRationale)
                    "We use your camera only to scan badge QR codes. Nothing leaves your device."
                else
                    "Camera permission was denied. Please enable it from your phone settings to scan badges.",
                fontSize = 14.sp, color = OnSurfaceSecondary, lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { cameraPermission.launchPermissionRequest() },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Allow Camera", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            if (!isSecretMode) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }) {
                    Text("Enter ID Manually Instead", color = BrandPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.weight(1f))
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, barcodeAnalyzer) }
                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder and Scan Line
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ViewfinderOverlay(modifier = Modifier.fillMaxSize())
            
            // Highly performant animated scan line using graphicsLayer for zero-lag drawing
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(2.dp)
                    .graphicsLayer { translationY = translateY.dp.toPx() }
                    .background(BrandPrimary.copy(alpha = 0.7f), CircleShape)
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Align the QR within the frame",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.offset(y = 145.dp)
            )
        }

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Row(
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isSecretMode) Icons.Filled.Key else Icons.Filled.QrCode,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSecretMode) "Scan Secret Key" else "Scan Verification",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                    .clickable { torchEnabled = !torchEnabled },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (torchEnabled) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOn,
                    contentDescription = "Torch",
                    tint = if (torchEnabled) BrandPrimary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom bar
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isSecretMode) {
                Row(
                    modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                        .clickable { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enter Manually", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            if (activeKeyLabel != null && !isSecretMode) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Using key: ", color = Color.White, fontSize = 12.sp)
                    Text(activeKeyLabel!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        if (verifying) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp).widthIn(min = 200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (isSecretMode) "Saving…" else "Verifying badge…",
                            fontWeight = FontWeight.SemiBold, color = OnSurface
                        )
                    }
                }
            }
        }
    }
}
