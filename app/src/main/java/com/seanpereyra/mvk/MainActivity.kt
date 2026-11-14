package com.seanpereyra.mvk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seanpereyra.mvk.data.MvkStore
import com.seanpereyra.mvk.ui.components.MvkBottomBar
import com.seanpereyra.mvk.ui.components.MvkHeader
import com.seanpereyra.mvk.ui.components.Tab
import com.seanpereyra.mvk.ui.screens.LibraryScreen
import com.seanpereyra.mvk.ui.screens.RandomizerScreen
import com.seanpereyra.mvk.ui.screens.ScanScreen
import com.seanpereyra.mvk.ui.screens.SettingsScreen
import com.seanpereyra.mvk.ui.screens.SplashScreen
import com.seanpereyra.mvk.ui.screens.TermsScreen
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkTheme

class MainActivity : ComponentActivity() {
    private val store by lazy { MvkStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by store.settings.collectAsStateWithLifecycle()
            val vouchers by store.vouchers.collectAsStateWithLifecycle()
            val termsAccepted by store.termsAccepted.collectAsStateWithLifecycle()
            var tab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
            var lastTab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
            var splashDone by rememberSaveable { mutableStateOf(false) }
            var showTerms by rememberSaveable { mutableStateOf(false) }

            fun go(t: Tab) { if (t != Tab.SETTINGS) lastTab = t; tab = t }
            BackHandler(enabled = tab == Tab.SETTINGS) { tab = lastTab }

            MvkTheme(settings) {
                when {
                    !splashDone -> { SplashScreen(onDone = { splashDone = true }); return@MvkTheme }
                    !termsAccepted -> {
                        TermsScreen(onAccept = { store.acceptTerms() }, onDecline = { finishAndRemoveTask() })
                        return@MvkTheme
                    }
                    showTerms -> {
                        BackHandler { showTerms = false }
                        TermsScreen(onAccept = { showTerms = false }, onDecline = { showTerms = false }, readOnly = true)
                        return@MvkTheme
                    }
                }
                val c = Mvk.colors
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(c.bg)
                        .statusBarsPadding()
                ) {
                    val barSpace = PaddingValues(bottom = 136.dp)
                    Column(Modifier.fillMaxSize()) {
                        if (tab == Tab.SETTINGS) MvkHeader(onBack = { tab = lastTab })
                        else MvkHeader(onSettings = { go(Tab.SETTINGS) })

                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tabs",
                            modifier = Modifier.navigationBarsPadding(),
                        ) { t ->
                            when (t) {
                                Tab.LIBRARY -> LibraryScreen(store, vouchers, onScan = { go(Tab.SCAN) }, contentPadding = barSpace)
                                Tab.SCAN -> ScanScreen(store, barSpace)
                                Tab.RANDOM -> RandomizerScreen(store, settings.sfx, barSpace)
                                Tab.SETTINGS -> SettingsScreen(store, settings, onShowTerms = { showTerms = true }, barSpace)
                            }
                        }
                    }
                    MvkBottomBar(
                        current = tab,
                        onSelect = ::go,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
    }
}
