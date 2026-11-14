package com.seanpereyra.mvk.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.seanpereyra.mvk.R
import com.seanpereyra.mvk.ui.components.MvkLogo
import com.seanpereyra.mvk.ui.theme.Mvk

/**
 * Icon intro shown when the app first opens: the launcher icon pops in, the wordmark fades up,
 * then it hands off to the app. Swap R.drawable.ic_launcher_foreground for the real logo later.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val c = Mvk.colors
    val iconScale = remember0()
    val iconAlpha = remember0()
    val wordAlpha = remember0()

    LaunchedEffect(Unit) {
        iconAlpha.animateTo(1f, tween(260))
        iconScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        wordAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        kotlinx.coroutines.delay(700)
        iconAlpha.animateTo(0f, tween(300))
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Mirrors the launcher icon: dark rounded square + the ticket foreground on top.
            Box(
                Modifier
                    .size(128.dp)
                    .scale(0.6f + iconScale.value * 0.4f)
                    .alpha(iconAlpha.value)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "MVK",
                    modifier = Modifier.size(150.dp),
                )
            }
            Spacer(Modifier.height(26.dp))
            Box(Modifier.alpha(wordAlpha.value)) { MvkLogo(big = true) }
        }
    }
}

@Composable
private fun remember0() = androidx.compose.runtime.remember { Animatable(0f) }
