package com.seanpereyra.mvk.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seanpereyra.mvk.BuildConfig
import com.seanpereyra.mvk.data.Accent
import com.seanpereyra.mvk.data.AppSettings
import com.seanpereyra.mvk.data.MvkStore
import com.seanpereyra.mvk.data.ThemeMode
import com.seanpereyra.mvk.data.UpdateChecker
import com.seanpereyra.mvk.ui.components.MvkLogo
import com.seanpereyra.mvk.ui.components.SectionLabel
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(store: MvkStore, settings: AppSettings, onShowTerms: () -> Unit, contentPadding: PaddingValues) {
    val c = Mvk.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateChecker.Result?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        Text("SETTINGS", style = MvkType.title, color = c.text, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))

        // ---------- Themes ----------
        SectionLabel("Themes")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                ThemeSwatch(mode, selected = settings.theme == mode, modifier = Modifier.weight(1f)) { store.setTheme(mode) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Accent color", style = MvkType.bodyMedium.copy(fontSize = 14.sp), color = c.text)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Accent.entries.forEach { a ->
                val color = if (a == Accent.MONO) c.text else Color(a.argb)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(if (settings.accent == a) 3.dp else 0.dp, c.text, CircleShape)
                            .padding(if (settings.accent == a) 5.dp else 0.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, c.line, CircleShape)
                            .clickable { store.setAccent(a) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (settings.accent == a) Icon(Icons.Rounded.Check, null, tint = if (a == Accent.MONO) c.bg else Color(0xFF111111), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(a.label, style = MvkType.small.copy(fontSize = 10.sp), color = c.textDim)
                }
            }
        }

        // ---------- Updates ----------
        Spacer(Modifier.height(28.dp))
        SectionLabel("App")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surface)
                .clickable(enabled = !checking) {
                    checking = true
                    scope.launch {
                        result = UpdateChecker.check(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
                        checking = false
                    }
                }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = c.text)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Check for updates", style = MvkType.bodyMedium, color = c.text)
                Text("You're on v${BuildConfig.VERSION_NAME}", style = MvkType.small, color = c.textDim)
            }
            if (checking) CircularProgressIndicator(Modifier.size(22.dp), color = c.text, strokeWidth = 2.5.dp)
            else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textDim)
        }

        // Sound effects toggle
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surface)
                .clickable { store.setSfx(!settings.sfx) }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.VolumeUp, null, tint = c.text)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Sound effects", style = MvkType.bodyMedium, color = c.text)
                Text("Plays while the randomizer spins", style = MvkType.small, color = c.textDim)
            }
            Switch(
                checked = settings.sfx,
                onCheckedChange = { store.setSfx(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.onAccent,
                    checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.textDim,
                    uncheckedTrackColor = c.surface,
                    uncheckedBorderColor = c.line,
                ),
            )
        }

        // ---------- About ----------
        Spacer(Modifier.height(28.dp))
        SectionLabel("About")
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, c.line, RoundedCornerShape(22.dp))
                .padding(vertical = 26.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MvkLogo(big = true)
            Spacer(Modifier.height(14.dp))
            Text(
                "Keep your school Wi-Fi vouchers in one place. Scan them, stack them, use them when you need them.",
                style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text("CREATED BY", style = MvkType.section, color = c.textDim)
            Spacer(Modifier.height(2.dp))
            Text("Sean Pereyra", style = MvkType.title.copy(fontSize = 28.sp), color = c.text)

            Spacer(Modifier.height(18.dp))
            SocialRow(Icons.Rounded.PhotoCamera, "Instagram", "@justseani1") {
                ctx.openUrl("https://instagram.com/justseani1")
            }
            Spacer(Modifier.height(8.dp))
            SocialRow(Icons.Rounded.AlternateEmail, "X (Twitter)", "@JustSEANI") {
                ctx.openUrl("https://x.com/JustSEANI")
            }
            Spacer(Modifier.height(8.dp))
            SocialRow(Icons.Rounded.MailOutline, "Email", "contactjustseani@gmail.com") {
                ctx.openUrl("mailto:contactjustseani@gmail.com")
            }
            Spacer(Modifier.height(8.dp))
            SocialRow(Icons.Rounded.Description, "Terms & Conditions", "Read the full terms", onShowTerms)

            Spacer(Modifier.height(16.dp))
            Text("Version ${BuildConfig.VERSION_NAME}", style = MvkType.small, color = c.textDim)
        }
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = {
                Text(
                    when (r) {
                        is UpdateChecker.Result.Available -> "Update available"
                        is UpdateChecker.Result.UpToDate -> "You're up to date"
                        is UpdateChecker.Result.Failed -> "Couldn't check"
                    }
                )
            },
            text = {
                Text(
                    when (r) {
                        is UpdateChecker.Result.Available -> "MVK ${r.versionName} is out.${if (r.notes.isNotBlank()) "\n\n${r.notes}" else ""}"
                        is UpdateChecker.Result.UpToDate -> "MVK v${r.current} is the latest version."
                        is UpdateChecker.Result.Failed -> r.reason
                    }
                )
            },
            confirmButton = {
                if (r is UpdateChecker.Result.Available && r.url.isNotBlank()) {
                    TextButton(onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url))) }
                        result = null
                    }) { Text("Download", color = c.text) }
                } else {
                    TextButton(onClick = { result = null }) { Text("OK", color = c.text) }
                }
            },
            dismissButton = if (r is UpdateChecker.Result.Available) {
                { TextButton(onClick = { result = null }) { Text("Later", color = c.textDim) } }
            } else null,
        )
    }
}

@Composable
private fun ThemeSwatch(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Mvk.colors
    val (bg, fg) = when (mode) {
        ThemeMode.DARK -> Color(0xFF1A1A1A) to Color.White
        ThemeMode.AMOLED -> Color.Black to Color.White
        ThemeMode.LIGHT -> Color(0xFFF2F1EC) to Color(0xFF141414)
        ThemeMode.SYSTEM -> Color(0xFF808080) to Color.White
    }
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(if (selected) 3.dp else 1.5.dp, if (selected) c.text else c.line, RoundedCornerShape(16.dp))
                .padding(if (selected) 5.dp else 3.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            if (mode == ThemeMode.SYSTEM) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFF1A1A1A)))
                    Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFFF2F1EC)))
                }
            }
            Text("Aa", style = MvkType.title.copy(fontSize = 22.sp), color = if (mode == ThemeMode.SYSTEM) Color(0xFF808080) else fg)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            mode.label.replace(" Black", ""),
            style = MvkType.small, color = if (selected) c.text else c.textDim,
        )
    }
}

@Composable
private fun SocialRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val c = Mvk.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, c.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MvkType.small.copy(fontSize = 11.sp), color = c.textDim)
            Text(value, style = MvkType.bodyMedium.copy(fontSize = 14.sp), color = c.text)
        }
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = c.textDim, modifier = Modifier.size(16.dp))
    }
}

private fun android.content.Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
