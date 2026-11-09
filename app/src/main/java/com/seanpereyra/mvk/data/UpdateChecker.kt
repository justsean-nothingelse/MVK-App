package com.seanpereyra.mvk.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * "Check for updates" reads a tiny JSON file you host yourself (e.g. the version.json in the
 * project root, pushed to a GitHub repo). Bump versionCode there when you release a new APK.
 *
 * >>> Change UPDATE_URL to your own raw GitHub link. <<<
 */
object UpdateChecker {
    const val UPDATE_URL =
        "https://raw.githubusercontent.com/YOUR_GITHUB_USERNAME/MVK/main/version.json"

    sealed interface Result {
        data class UpToDate(val current: String) : Result
        data class Available(val versionName: String, val url: String, val notes: String) : Result
        data class Failed(val reason: String) : Result
    }

    suspend fun check(currentCode: Int, currentName: String): Result = withContext(Dispatchers.IO) {
        if (UPDATE_URL.contains("YOUR_GITHUB_USERNAME")) {
            return@withContext Result.Failed("Update server not set up yet.")
        }
        runCatching {
            val conn = (URL(UPDATE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                useCaches = false
            }
            try {
                if (conn.responseCode != 200) error("Server replied ${conn.responseCode}")
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val latest = json.getInt("versionCode")
                if (latest > currentCode) Result.Available(
                    versionName = json.optString("versionName", "v$latest"),
                    url = json.optString("url"),
                    notes = json.optString("notes"),
                ) else Result.UpToDate(currentName)
            } finally {
                conn.disconnect()
            }
        }.getOrElse { Result.Failed("No connection. Try again on mobile data or Wi-Fi.") }
    }
}
