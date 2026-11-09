package com.seanpereyra.mvk.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.seanpereyra.mvk.data.MvkStore
import com.seanpereyra.mvk.data.Voucher
import com.seanpereyra.mvk.ui.components.VoucherFormSheet
import com.seanpereyra.mvk.ui.components.VoucherTicket
import com.seanpereyra.mvk.ui.components.copyCode
import com.seanpereyra.mvk.ui.components.openWifi
import com.seanpereyra.mvk.ui.components.toast
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType

@Composable
fun LibraryScreen(store: MvkStore, vouchers: List<Voucher>, onScan: () -> Unit, contentPadding: PaddingValues) {
    val c = Mvk.colors
    val ctx = LocalContext.current
    var showUsed by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Voucher?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val available = vouchers.filterNot { it.used }
    val used = vouchers.filter { it.used }
    val shown = if (showUsed) used else available

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("VOUCHERS", style = MvkType.title, color = c.text)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Segmented(
                        left = "Available ${available.size}", right = "Used ${used.size}",
                        rightSelected = showUsed, onChange = { showUsed = it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(c.accent)
                            .clickable { editing = Voucher(code = "") },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, "Add manually", tint = c.onAccent) }
                }
                if (showUsed && used.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear all used", style = MvkType.small, color = c.textDim)
                    }
                }
            }
        }

        if (shown.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                EmptyState(
                    if (showUsed) "Nothing used yet" else "No vouchers yet",
                    if (showUsed) "Vouchers you mark as used will show up here." else "Scan a voucher slip and it lands here, ready when you need Wi-Fi.",
                    action = if (showUsed) null else ("Scan a voucher" to onScan),
                )
            }
        } else {
            items(shown, key = { it.id }) { v ->
                VoucherTicket(v, Modifier.animateItem()) { selectedId = v.id }
            }
        }
    }

    // ---- actions sheet ----
    val selected = vouchers.firstOrNull { it.id == selectedId }
    if (selected != null) {
        VoucherActionsSheet(
            v = selected,
            onDismiss = { selectedId = null },
            onCopy = { ctx.copyCode(selected.code) },
            onConnect = { ctx.copyCode(selected.code); ctx.openWifi() },
            onToggleUsed = { store.update(selected.copy(used = !selected.used)); selectedId = null },
            onEdit = { editing = selected; selectedId = null },
            onDelete = { store.delete(selected.id); selectedId = null; ctx.toast("Voucher deleted") },
        )
    }

    editing?.let { v ->
        val isNew = vouchers.none { it.id == v.id }
        VoucherFormSheet(
            title = if (isNew) "Add voucher" else "Edit voucher",
            initial = v,
            confirmLabel = if (isNew) "Save to library" else "Save changes",
            onSave = {
                if (isNew) {
                    if (store.add(it)) ctx.toast("Saved to library") else ctx.toast("That code is already saved")
                } else store.update(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear used vouchers?") },
            text = { Text("This removes ${used.size} used voucher(s) from your library.") },
            confirmButton = { TextButton(onClick = { store.clearUsed(); confirmClear = false }) { Text("Clear", color = Color(0xFFE5484D)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = c.text) } },
        )
    }
}

@Composable
private fun Segmented(left: String, right: String, rightSelected: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Mvk.colors
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .padding(4.dp)
    ) {
        listOf(left to false, right to true).forEach { (label, isRight) ->
            val sel = rightSelected == isRight
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (sel) c.text else Color.Transparent)
                    .clickable { onChange(isRight) }
                    .animateContentSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MvkType.small.copy(fontSize = 13.sp), color = if (sel) c.bg else c.textDim)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, action: Pair<String, () -> Unit>?) {
    val c = Mvk.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .border(2.dp, c.line, RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("¯\\_(ツ)_/¯", style = MvkType.code.copy(fontSize = 22.sp), color = c.textDim)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MvkType.title.copy(fontSize = 24.sp), color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = action.second,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
            ) { Text(action.first, style = MvkType.bodyMedium) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoucherActionsSheet(
    v: Voucher,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onConnect: () -> Unit,
    onToggleUsed: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Mvk.colors
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                listOf(v.network, v.slot.takeIf { it.isNotBlank() }?.let { "[$it]" }, v.duration.takeIf { it.isNotBlank() })
                    .filterNotNull().filter { it.isNotBlank() }.joinToString("  ·  "),
                style = MvkType.section, color = c.textDim,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                v.code,
                style = MvkType.code.copy(fontSize = 46.sp, letterSpacing = 0.18.em),
                color = c.text,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .border(3.dp, c.text, RoundedCornerShape(18.dp))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 26.dp, vertical = 14.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text("Tap the code to copy", style = MvkType.small, color = c.textDim)
            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Rounded.Wifi, null)
                Spacer(Modifier.width(8.dp))
                Text("COPY & OPEN WI-FI", style = MvkType.button.copy(fontSize = 18.sp))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile(Icons.Rounded.ContentCopy, "Copy", Modifier.weight(1f), onCopy)
                ActionTile(if (v.used) Icons.Rounded.Replay else Icons.Rounded.CheckCircle, if (v.used) "Unmark" else "Used", Modifier.weight(1f), onToggleUsed)
                ActionTile(Icons.Rounded.Edit, "Edit", Modifier.weight(1f), onEdit)
                ActionTile(Icons.Rounded.DeleteOutline, "Delete", Modifier.weight(1f), danger = true) { confirmDelete = true }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${v.code}?") },
            text = { Text("It will be removed from your library.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = Color(0xFFE5484D)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = c.text) } },
        )
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) =
    ActionTile(icon, label, modifier, false, onClick)

@Composable
private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier, danger: Boolean, onClick: () -> Unit) {
    val c = Mvk.colors
    val tint = if (danger) Color(0xFFE5484D) else c.text
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, c.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MvkType.small, color = tint)
    }
}
