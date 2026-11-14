package com.seanpereyra.mvk.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.seanpereyra.mvk.update.AppUpdater
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private sealed interface Phase {
    data object NeedPermission : Phase
    data class Downloading(val read: Long, val total: Long) : Phase
    data object Installing : Phase
    data object Cancelled : Phase
    data object Done : Phase
    data class Error(val message: String) : Phase
}

/** Downloads and installs an update inside the app, with a progress bar. No browser. */
@Composable
fun UpdateFlowDialog(versionName: String, url: String, onClose: () -> Unit) {
    val c = Mvk.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf<Phase>(Phase.Downloading(0, -1)) }
    var job by remember { mutableStateOf<Job?>(null) }
    val event by AppUpdater.events.collectAsState()

    fun install(file: File) {
        phase = Phase.Installing
        runCatching { AppUpdater.install(ctx, file) }
            .onFailure { phase = Phase.Error(it.message ?: "Couldn't start the install.") }
    }

    fun start() {
        if (!AppUpdater.canInstall(ctx)) { phase = Phase.NeedPermission; return }
        // Already downloaded (e.g. you cancelled Android's dialog)? Skip straight to installing.
        val cached = AppUpdater.updateFile(ctx)
        if (cached.exists() && runCatching { AppUpdater.verify(ctx, cached) }.isSuccess) { install(cached); return }
        phase = Phase.Downloading(0, -1)
        job?.cancel()
        job = scope.launch {
            try {
                val file = AppUpdater.download(ctx, url) { read, total -> phase = Phase.Downloading(read, total) }
                install(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                phase = Phase.Error(e.message?.takeIf { it.endsWith(".") } ?: "Download failed. Check your internet connection.")
            }
        }
    }

    fun close() { job?.cancel(); onClose() }

    LaunchedEffect(Unit) { start() }

    // Results from Android's installer (only while we're actually installing).
    LaunchedEffect(event) {
        if (phase != Phase.Installing) return@LaunchedEffect
        when (val e = event) {
            AppUpdater.InstallEvent.Cancelled -> phase = Phase.Cancelled
            is AppUpdater.InstallEvent.Failed -> phase = Phase.Error(e.message)
            AppUpdater.InstallEvent.Succeeded -> phase = Phase.Done
            AppUpdater.InstallEvent.WaitingForConfirm, null -> Unit
        }
    }

    // Coming back from the "Install unknown apps" screen: continue automatically if allowed.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && phase == Phase.NeedPermission && AppUpdater.canInstall(ctx)) start()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val busy = phase is Phase.Downloading || phase == Phase.Installing
    AlertDialog(
        onDismissRequest = { if (!busy) close() },
        properties = DialogProperties(dismissOnClickOutside = false),
        containerColor = c.surface,
        title = {
            Text(
                when (phase) {
                    Phase.NeedPermission -> "Allow MVK to update itself"
                    Phase.Done -> "Updated!"
                    else -> "Updating to v$versionName"
                },
                color = c.text,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                when (val p = phase) {
                    Phase.NeedPermission -> Body(
                        "Android needs your OK once before MVK can install its own updates.\n\n" +
                            "Tap Open settings, turn on \"Allow from this source\", then come back. The update continues by itself."
                    )
                    is Phase.Downloading -> {
                        val fraction = if (p.total > 0) (p.read.toFloat() / p.total).coerceIn(0f, 1f) else null
                        Body(if (fraction == null) "Downloading… ${mb(p.read)}" else "Downloading… ${mb(p.read)} of ${mb(p.total)}")
                        Spacer(Modifier.height(14.dp))
                        if (fraction == null) {
                            LinearProgressIndicator(color = c.text, trackColor = c.line, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(progress = { fraction }, color = c.text, trackColor = c.line, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text("${(fraction * 100).toInt()}%", style = MvkType.small, color = c.textDim)
                        }
                    }
                    Phase.Installing -> {
                        Body("Installing… If Android asks, tap Update. MVK will close when it's done. Just open it again.")
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(color = c.text, trackColor = c.line, modifier = Modifier.fillMaxWidth())
                    }
                    Phase.Cancelled -> Body("The update was cancelled. Your vouchers are untouched. You can try again anytime.")
                    Phase.Done -> Body("MVK v$versionName is installed. Open the app again to use it.")
                    is Phase.Error -> Body(p.message)
                }
            }
        },
        confirmButton = {
            when (phase) {
                Phase.NeedPermission -> TextButton(onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")))
                    }
                }) { Text("Open settings", color = c.text) }
                is Phase.Downloading -> TextButton(onClick = ::close) { Text("Cancel", color = c.text) }
                Phase.Installing -> TextButton(onClick = ::close) { Text("Hide", color = c.textDim) }
                Phase.Cancelled, is Phase.Error -> TextButton(onClick = ::start) { Text("Try again", color = c.text) }
                Phase.Done -> TextButton(onClick = ::close) { Text("OK", color = c.text) }
            }
        },
        dismissButton = when (phase) {
            Phase.NeedPermission, Phase.Cancelled, is Phase.Error -> {
                { TextButton(onClick = ::close) { Text(if (phase == Phase.NeedPermission) "Not now" else "Close", color = c.textDim) } }
            }
            else -> null
        },
    )
}

@Composable
private fun Body(text: String) {
    Text(text, style = MvkType.body.copy(fontSize = 14.sp), color = Mvk.colors.textDim)
}

private fun mb(bytes: Long) = String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
