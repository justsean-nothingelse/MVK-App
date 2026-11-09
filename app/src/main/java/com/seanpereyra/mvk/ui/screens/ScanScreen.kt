package com.seanpereyra.mvk.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.seanpereyra.mvk.data.MvkStore
import com.seanpereyra.mvk.data.Voucher
import com.seanpereyra.mvk.scan.VoucherParser
import com.seanpereyra.mvk.ui.components.VoucherFormSheet
import com.seanpereyra.mvk.ui.components.toast
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScanScreen(store: MvkStore, contentPadding: PaddingValues) {
    val c = Mvk.colors
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it; askedOnce = true
    }

    var found by remember { mutableStateOf<Voucher?>(null) }
    var manual by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val paused = remember { AtomicBoolean(false) }
    paused.set(found != null || manual)

    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Outer white frame, like the mockup
        Box(
            Modifier
                .padding(horizontal = 26.dp)
                .padding(top = 8.dp)
                .weight(1f)
                .fillMaxWidth()
                .border(5.dp, c.text, RoundedCornerShape(34.dp))
                .padding(14.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (hasPermission) {
                CameraPreview(
                    paused = paused,
                    onCamera = { camera = it },
                    onDetected = { r ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        found = Voucher(code = r.code, network = r.network.ifBlank { "CICTLAB" }, slot = r.slot, duration = r.duration)
                    },
                )
                ScanOverlay()
            } else {
                PermissionPrompt(
                    permanentlyDenied = askedOnce,
                    onAsk = { launcher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                    },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 26.dp)) {
            PillButton(
                icon = if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                label = if (torch) "Light on" else "Light",
                enabled = hasPermission && camera?.cameraInfo?.hasFlashUnit() == true,
                modifier = Modifier.weight(1f),
            ) {
                torch = !torch
                camera?.cameraControl?.enableTorch(torch)
            }
            PillButton(Icons.Rounded.Keyboard, "Type it in", modifier = Modifier.weight(1.3f)) { manual = true }
        }
    }

    found?.let { v ->
        VoucherFormSheet(
            title = "Got one!",
            hint = "Check the code matches the slip, then save.",
            initial = v,
            onSave = {
                if (store.add(it)) ctx.toast("Saved to library") else ctx.toast("That code is already saved")
                found = null
            },
            onDismiss = { found = null },
        )
    }
    if (manual) {
        VoucherFormSheet(
            title = "Add voucher",
            initial = Voucher(code = ""),
            onSave = {
                if (store.add(it)) ctx.toast("Saved to library") else ctx.toast("That code is already saved")
                manual = false
            },
            onDismiss = { manual = false },
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(paused: AtomicBoolean, onCamera: (Camera) -> Unit, onDetected: (VoucherParser.Result) -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnDetected by rememberUpdatedState(onDetected)
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        var lastCode: String? = null
        var streak = 0

        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || paused.get()) { proxy.close(); return@setAnalyzer }
                val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                recognizer.process(input)
                    .addOnSuccessListener { text ->
                        val lines = text.textBlocks.flatMap { b -> b.lines.map { it.text } }
                        val r = VoucherParser.parse(lines)
                        if (r != null && r.code == lastCode) streak++ else { streak = if (r != null) 1 else 0 }
                        lastCode = r?.code
                        // Same code read twice in a row = confident enough
                        if (r != null && streak >= 2 && !paused.get()) {
                            paused.set(true)
                            streak = 0; lastCode = null
                            previewView.post { latestOnDetected(r) }
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                provider.unbindAll()
                val cam = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                onCamera(cam)
            }
        }, ContextCompat.getMainExecutor(ctx))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            recognizer.close()
            executor.shutdown()
        }
    }

    AndroidView({ previewView }, Modifier.fillMaxSize())
}

@Composable
private fun ScanOverlay() {
    val pulse = rememberInfiniteTransition(label = "scan")
    val dots by pulse.animateFloat(0f, 3.99f, infiniteRepeatable(tween(1400)), label = "dots")
    val line by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "line")

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val boxW = maxWidth * 0.66f
        val boxH = boxW * 0.62f
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(boxW, boxH)
                    .border(4.dp, Color.White, RoundedCornerShape(22.dp))
            ) {
                Box(
                    Modifier
                        .offset(y = (boxH - 3.dp) * line)
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "SCANNING" + ".".repeat(dots.toInt()),
                style = MvkType.button.copy(fontSize = 20.sp, letterSpacing = 0.08.em),
                color = Color.White,
                modifier = Modifier.width(boxW),
                textAlign = TextAlign.Center,
            )
            Text("Fit the voucher slip inside the box", style = MvkType.small, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun PermissionPrompt(permanentlyDenied: Boolean, onAsk: () -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.NoPhotography, null, tint = Color.White, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text("Camera needed", style = MvkType.title.copy(fontSize = 26.sp), color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "MVK uses the camera only to read voucher codes. Nothing leaves your phone.",
            style = MvkType.body.copy(fontSize = 14.sp), color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = if (permanentlyDenied) onOpenSettings else onAsk,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) { Text(if (permanentlyDenied) "Open settings" else "Allow camera", style = MvkType.bodyMedium) }
    }
}

@Composable
private fun PillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = Mvk.colors
    Row(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(c.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (enabled) c.text else c.textDim.copy(alpha = 0.5f)
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MvkType.bodyMedium.copy(fontSize = 14.sp), color = tint)
    }
}
