package com.seanpereyra.mvk.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.seanpereyra.mvk.data.Voucher
import com.seanpereyra.mvk.ui.theme.Body
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType

enum class Tab { LIBRARY, SCAN, RANDOM, SETTINGS }

// ---------------- Logo + header ----------------

@Composable
fun MvkLogo(modifier: Modifier = Modifier, big: Boolean = false) {
    val c = Mvk.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("MVK", style = MvkType.logo.copy(fontSize = if (big) 56.sp else 30.sp), color = c.text)
            Box(
                Modifier
                    .padding(start = 3.dp, bottom = if (big) 12.dp else 7.dp)
                    .size(if (big) 12.dp else 7.dp)
                    .clip(CircleShape)
                    .background(c.accent.takeIf { it != c.text } ?: c.text)
            )
        }
        Text(
            "MAY VOUCHER KA?",
            style = MvkType.section.copy(fontSize = if (big) 12.sp else 8.5.sp, letterSpacing = 0.3.em),
            color = c.textDim,
        )
    }
}

@Composable
fun MvkHeader(onSettings: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    val c = Mvk.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.text)
            }
        }
        MvkLogo(Modifier.align(Alignment.Center))
        if (onSettings != null) {
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Rounded.Settings, "Settings", tint = c.textDim)
            }
        }
    }
}

// ---------------- Bottom bar ----------------

@Composable
fun MvkBottomBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Mvk.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(c.nav)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(Icons.Rounded.CollectionsBookmark, "Library", current == Tab.LIBRARY) { onSelect(Tab.LIBRARY) }
        ScanNavItem(current == Tab.SCAN) { onSelect(Tab.SCAN) }
        NavItem(Icons.Rounded.Casino, "Random", current == Tab.RANDOM) { onSelect(Tab.RANDOM) }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Mvk.colors
    val tint by animateColorAsState(if (selected) Color.White else Color.White.copy(alpha = 0.5f), label = "tint")
    Column(
        Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MvkType.small.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), color = tint)
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(width = if (selected) 16.dp else 0.dp, height = 3.dp)
                .clip(CircleShape)
                .background(if (c.accent == Color.Black) Color.White else c.accent)
        )
    }
}

@Composable
private fun ScanNavItem(selected: Boolean, onClick: () -> Unit) {
    val c = Mvk.colors
    val size by animateDpAsState(if (selected) 66.dp else 60.dp, label = "scanSize")
    val pillBg = if (c.accent == Color.Black) Color.White else c.accent
    val pillFg = if (c.accent == Color.Black) Color.Black else c.onAccent
    Column(
        Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(22.dp))
                .background(if (selected) pillBg else Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.DocumentScanner, "Scan", tint = if (selected) pillFg else Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("Scan", style = MvkType.small.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

// ---------------- Voucher ticket (looks like the paper slip) ----------------

@Composable
fun VoucherTicket(v: Voucher, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val ink = Color(0xFF111111)
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .alpha(if (v.used) 0.45f else 1f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(2.5.dp, ink, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    v.network.ifBlank { "VOUCHER" },
                    fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (v.slot.isNotBlank()) {
                    Text("[${v.slot}]", fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ink)
                }
            }
            HorizontalDivider(thickness = 2.dp, color = ink)
            Text(
                "Voucher Code",
                fontFamily = Body, fontSize = 11.sp, color = ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 3.dp),
            )
            Text(
                v.code,
                style = MvkType.code.copy(fontSize = 21.sp, letterSpacing = 0.06.em),
                color = ink, textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ink)
                    .padding(vertical = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                v.duration.ifBlank { "—" },
                fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ink)
                    .padding(vertical = 1.dp),
            )
        }
        if (v.used) {
            Text(
                "USED",
                style = MvkType.button.copy(fontSize = 26.sp, letterSpacing = 0.12.em),
                color = Color(0xFFD7263D),
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(-14f)
                    .border(3.dp, Color(0xFFD7263D), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

// ---------------- Helpers ----------------

fun Context.copyCode(code: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Voucher code", code))
    // Android 13+ shows its own "Copied" bubble.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast("Copied $code")
}

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

/** Opens the Wi-Fi picker so you can join the school network and paste the code in the login page. */
fun Context.openWifi() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
    else Intent(Settings.ACTION_WIFI_SETTINGS)
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MvkType.section, color = Mvk.colors.textDim, modifier = modifier)
}
