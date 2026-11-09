package com.seanpereyra.mvk.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray

enum class ThemeMode(val label: String) {
    SYSTEM("System"), DARK("Dark"), AMOLED("AMOLED Black"), LIGHT("Light")
}

enum class Accent(val label: String, val argb: Long) {
    MONO("Mono", 0xFFFFFFFF),
    LIME("Lime", 0xFFC6F432),
    SKY("Sky", 0xFF5BC8FF),
    CORAL("Coral", 0xFFFF6B5B),
    VIOLET("Violet", 0xFFB18CFF),
    SUN("Sun", 0xFFFFC93C),
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val accent: Accent = Accent.MONO,
)

/** Everything is stored on the phone only. No accounts, no cloud. */
class MvkStore(context: Context) {
    private val prefs = context.getSharedPreferences("mvk", Context.MODE_PRIVATE)

    private val _vouchers = MutableStateFlow(loadVouchers())
    val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // ---- vouchers ----
    /** @return false if that code is already in the library */
    fun add(v: Voucher): Boolean {
        if (_vouchers.value.any { it.code == v.code }) return false
        _vouchers.update { listOf(v) + it }
        saveVouchers()
        return true
    }

    fun update(v: Voucher) {
        _vouchers.update { list -> list.map { if (it.id == v.id) v else it } }
        saveVouchers()
    }

    fun delete(id: String) {
        _vouchers.update { list -> list.filterNot { it.id == id } }
        saveVouchers()
    }

    fun clearUsed() {
        _vouchers.update { list -> list.filterNot { it.used } }
        saveVouchers()
    }

    private fun loadVouchers(): List<Voucher> = runCatching {
        val arr = JSONArray(prefs.getString("vouchers", "[]"))
        List(arr.length()) { Voucher.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun saveVouchers() {
        val arr = JSONArray().apply { _vouchers.value.forEach { put(it.toJson()) } }
        prefs.edit().putString("vouchers", arr.toString()).apply()
    }

    // ---- settings ----
    fun setTheme(t: ThemeMode) { _settings.update { it.copy(theme = t) }; saveSettings() }
    fun setAccent(a: Accent) { _settings.update { it.copy(accent = a) }; saveSettings() }

    private fun loadSettings() = AppSettings(
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "DARK")!!) }.getOrDefault(ThemeMode.DARK),
        accent = runCatching { Accent.valueOf(prefs.getString("accent", "MONO")!!) }.getOrDefault(Accent.MONO),
    )

    private fun saveSettings() {
        prefs.edit()
            .putString("theme", _settings.value.theme.name)
            .putString("accent", _settings.value.accent.name)
            .apply()
    }
}
