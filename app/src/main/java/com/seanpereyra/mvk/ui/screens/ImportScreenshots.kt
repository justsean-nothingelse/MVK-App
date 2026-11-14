package com.seanpereyra.mvk.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as MlText
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.seanpereyra.mvk.data.MvkStore
import com.seanpereyra.mvk.data.Voucher
import com.seanpereyra.mvk.scan.ImportedVoucher
import com.seanpereyra.mvk.scan.OcrLine
import com.seanpereyra.mvk.scan.ScreenshotImport
import com.seanpereyra.mvk.ui.components.toast
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class Found(val v: ImportedVoucher, val alreadySaved: Boolean) {
    var selected by mutableStateOf(!alreadySaved)
}

/**
 * Reads the picked screenshots of an MVK library, then lets you review and import the vouchers.
 * Made for moving vouchers out of the first MVK build, which can't be updated in place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreenshotsSheet(uris: List<Uri>, store: MvkStore, existingCodes: Set<String>, onClose: () -> Unit) {
    val c = Mvk.colors
    val ctx = LocalContext.current
    var done by remember { mutableIntStateOf(0) }
    var reading by remember { mutableStateOf(true) }
    var failed by remember { mutableIntStateOf(0) }
    val found = remember { mutableStateListOf<Found>() }

    LaunchedEffect(uris) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val seen = mutableSetOf<String>()
        try {
            for (uri in uris) {
                val lines = runCatching { readLines(ctx, uri, recognizer) }.getOrElse { failed++; emptyList() }
                ScreenshotImport.parse(lines).forEach { v ->
                    if (seen.add(v.code)) found += Found(v, alreadySaved = v.code in existingCodes)
                }
                done++
            }
        } finally {
            recognizer.close()
            reading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Text("Import vouchers", style = MvkType.title.copy(fontSize = 30.sp), color = c.text)
            Spacer(Modifier.height(6.dp))

            if (reading) {
                Text("Reading screenshot ${(done + 1).coerceAtMost(uris.size)} of ${uris.size}…", style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim)
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { done / uris.size.toFloat().coerceAtLeast(1f) },
                    color = c.text, trackColor = c.line,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            val newOnes = found.count { !it.alreadySaved }
            val selected = found.filter { it.selected }
            Text(
                when {
                    found.isEmpty() -> "No vouchers found. Use screenshots of your old MVK library screen, with the voucher tickets fully visible."
                    else -> "Found ${found.size} voucher${if (found.size == 1) "" else "s"}" +
                        (if (found.size != newOnes) ", ${found.size - newOnes} already saved" else "") +
                        ". Check the codes against your old app before importing."
                },
                style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim,
            )
            if (failed > 0) {
                Spacer(Modifier.height(4.dp))
                Text("$failed screenshot(s) couldn't be opened.", style = MvkType.small, color = Color(0xFFE5484D))
            }
            Spacer(Modifier.height(12.dp))

            if (found.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                ) {
                    items(found, key = { it.v.code }) { f -> FoundRow(f) }
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                ) { Text(if (found.isEmpty()) "Close" else "Cancel", style = MvkType.bodyMedium, color = c.text) }
                if (found.isNotEmpty()) {
                    Button(
                        onClick = {
                            // Add in reverse so the first ticket in the screenshots ends up on top.
                            var added = 0
                            selected.asReversed().forEach { f ->
                                val v = f.v
                                if (store.add(Voucher(code = v.code, network = v.network.ifBlank { "CICTLAB" }, slot = v.slot, duration = v.duration, used = v.used))) added++
                            }
                            ctx.toast("Imported $added voucher${if (added == 1) "" else "s"}")
                            onClose()
                        },
                        enabled = selected.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(54.dp),
                    ) { Text("Import ${selected.size}", style = MvkType.bodyMedium.copy(fontSize = 15.sp)) }
                }
            }
        }
    }
}

@Composable
private fun FoundRow(f: Found) {
    val c = Mvk.colors
    val v = f.v
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, if (f.selected) c.text else c.line, RoundedCornerShape(14.dp))
            .clickable(enabled = !f.alreadySaved) { f.selected = !f.selected }
            .padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = f.selected,
            onCheckedChange = if (f.alreadySaved) null else { on -> f.selected = on },
            enabled = !f.alreadySaved,
            colors = CheckboxDefaults.colors(checkedColor = c.text, checkmarkColor = c.bg, uncheckedColor = c.textDim),
        )
        Column(Modifier.weight(1f)) {
            Text(v.code, style = MvkType.code.copy(fontSize = 20.sp, letterSpacing = 0.1.em), color = if (f.alreadySaved) c.textDim else c.text)
            val meta = listOf(v.network, v.slot.takeIf { it.isNotBlank() }?.let { "[$it]" }, v.duration.takeIf { it.isNotBlank() })
                .filterNotNull().filter { it.isNotBlank() }.joinToString("  ·  ")
            Text(if (f.alreadySaved) "Already in your library" else meta.ifBlank { "No details" }, style = MvkType.small, color = c.textDim)
        }
        if (v.used) {
            Spacer(Modifier.width(8.dp))
            Text(
                "USED",
                style = MvkType.small.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.1.em),
                color = Color(0xFFD7263D),
                modifier = Modifier
                    .border(1.5.dp, Color(0xFFD7263D), RoundedCornerShape(6.dp))
                    .background(Color(0x14D7263D), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

private suspend fun readLines(ctx: Context, uri: Uri, recognizer: TextRecognizer): List<OcrLine> {
    val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(ctx, uri) }
    val text = recognizer.read(image)
    return text.textBlocks.flatMap { block ->
        block.lines.mapNotNull { line ->
            line.boundingBox?.let { r -> OcrLine(line.text, r.left, r.top, r.right, r.bottom) }
        }
    }
}

private suspend fun TextRecognizer.read(image: InputImage): MlText = suspendCancellableCoroutine { cont ->
    process(image)
        .addOnSuccessListener { cont.resume(it) }
        .addOnFailureListener { cont.resumeWithException(it) }
}
