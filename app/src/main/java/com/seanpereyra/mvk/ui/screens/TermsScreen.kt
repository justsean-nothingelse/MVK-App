package com.seanpereyra.mvk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seanpereyra.mvk.ui.components.MvkLogo
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType

/**
 * Shown once, right after the splash, until the user accepts. Choosing "Decline" closes the app.
 * Also reachable read-only from Settings → About.
 */
@Composable
fun TermsScreen(onAccept: () -> Unit, onDecline: () -> Unit, readOnly: Boolean = false) {
    val c = Mvk.colors
    val ctx = LocalContext.current
    val lines = remember { loadTerms(ctx) }
    val listState = rememberLazyListState()

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MvkLogo()
            Spacer(Modifier.height(14.dp))
            Text("TERMS & CONDITIONS", style = MvkType.title.copy(fontSize = 26.sp), color = c.text)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(lines) { line -> TermsLine(line) }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Footer buttons
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            if (readOnly) {
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) { Text("Close", style = MvkType.bodyMedium) }
            } else {
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) { Text("I AGREE", style = MvkType.button.copy(fontSize = 20.sp)) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDecline, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Decline and exit", style = MvkType.small, color = c.textDim)
                }
            }
        }
    }
}

@Composable
private fun TermsLine(line: String) {
    val c = Mvk.colors
    when {
        line.isBlank() -> Spacer(Modifier.height(6.dp))
        line.startsWith("# ") -> Text(
            line.removePrefix("# ").clean(),
            style = MvkType.title.copy(fontSize = 22.sp), color = c.text,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        line.startsWith("## ") -> Text(
            line.removePrefix("## ").clean(),
            style = MvkType.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = c.text,
            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
        )
        line.startsWith("---") -> Spacer(Modifier.height(4.dp))
        line.startsWith("- ") -> Row(Modifier.padding(start = 6.dp, top = 2.dp)) {
            Text("•  ", style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim)
            Text(line.removePrefix("- ").clean(), style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim)
        }
        else -> Text(line.clean(), style = MvkType.body.copy(fontSize = 14.sp), color = c.textDim)
    }
}

/** Strip the **bold** markers; keep the words. */
private fun String.clean() = replace("**", "").trim()

private fun loadTerms(ctx: android.content.Context): List<String> = runCatching {
    ctx.assets.open("terms.md").bufferedReader().use { it.readText() }.split("\n")
}.getOrDefault(listOf("Terms unavailable."))
