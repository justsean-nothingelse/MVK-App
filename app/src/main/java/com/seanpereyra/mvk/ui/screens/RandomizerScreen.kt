package com.seanpereyra.mvk.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seanpereyra.mvk.data.MvkStore
import com.seanpereyra.mvk.data.Sfx
import com.seanpereyra.mvk.data.Voucher
import com.seanpereyra.mvk.scan.VoucherParser
import com.seanpereyra.mvk.ui.components.SectionLabel
import com.seanpereyra.mvk.ui.components.copyCode
import com.seanpereyra.mvk.ui.components.openWifi
import com.seanpereyra.mvk.ui.components.toast
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom

private const val POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
private val rng = SecureRandom()

/** 5 random characters from A–Z, a–z, 0–9. */
fun randomCode(len: Int = VoucherParser.CODE_LENGTH) = buildString { repeat(len) { append(POOL[rng.nextInt(POOL.length)]) } }

@Composable
fun RandomizerScreen(store: MvkStore, sfxEnabled: Boolean, contentPadding: PaddingValues) {
    val c = Mvk.colors
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sfx = remember { Sfx(scope) }
    sfx.enabled = sfxEnabled

    var code by rememberSaveable { mutableStateOf(randomCode()) }
    var rolling by remember { mutableStateOf(false) }
    var tries by rememberSaveable { mutableStateOf(0) }
    val pop = remember { Animatable(1f) }

    fun roll() {
        if (rolling) return
        scope.launch {
            rolling = true
            val final = randomCode()
            var lockedSoFar = 0
            // Slot-machine effect: characters lock in left to right
            for (frame in 0 until 14) {
                val locked = (frame - 4).coerceAtLeast(0) / 2
                code = final.take(locked) + randomCode(final.length - locked)
                if (locked > lockedSoFar) { sfx.tick(); lockedSoFar = locked } else sfx.blip()
                delay(38)
            }
            code = final
            tries++
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sfx.win()
            pop.snapTo(1.08f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            rolling = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding())
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionLabel("Random code")
        Spacer(Modifier.height(4.dp))
        Text(
            "Out of vouchers? Try your luck.",
            style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))

        // Code box
        Box(
            Modifier
                .fillMaxWidth()
                .scale(pop.value)
                .clip(RoundedCornerShape(22.dp))
                .border(5.dp, c.text, RoundedCornerShape(22.dp))
                .clickable { ctx.copyCode(code) }
                .padding(vertical = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                code.forEach { ch ->
                    Text(
                        ch.toString(),
                        style = MvkType.code.copy(fontSize = 44.sp),
                        color = c.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tap the code to copy", style = MvkType.small, color = c.textDim)

        Spacer(Modifier.height(26.dp))
        GoButton(onClick = ::roll)

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallAction(Icons.Rounded.Wifi, "Copy & open Wi-Fi") { ctx.copyCode(code); ctx.openWifi() }
            SmallAction(Icons.Rounded.BookmarkAdd, "It worked!") {
                if (store.add(Voucher(code = code, network = "CICTLAB", duration = ""))) ctx.toast("Nice! Saved to library")
                else ctx.toast("Already in your library")
            }
        }
        if (tries > 0) {
            Spacer(Modifier.height(14.dp))
            Text("$tries ${if (tries == 1) "try" else "tries"} this session", style = MvkType.small, color = c.textDim)
        }
    }
}

@Composable
private fun GoButton(onClick: () -> Unit) {
    val c = Mvk.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .fillMaxWidth(0.78f)
            .height(84.dp)
            .scale(if (pressed) 0.96f else 1f)
            .clip(RoundedCornerShape(22.dp))
            .background(c.accent)
            .clickable(source, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("GO!", style = MvkType.button.copy(fontSize = 38.sp), color = c.onAccent)
    }
}

@Composable
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = Mvk.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, c.line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MvkType.small.copy(fontSize = 13.sp), color = c.text)
    }
}
