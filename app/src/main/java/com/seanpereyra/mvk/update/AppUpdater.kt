package com.seanpereyra.mvk.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.IntentCompat
import com.seanpereyra.mvk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the new APK inside the app and installs it through Android's PackageInstaller,
 * so no browser is involved. Android still asks you to confirm the install (unless it lets
 * the app update itself silently, which newer Android versions may allow).
 */
object AppUpdater {
    /** What happened to the last install attempt, reported by [InstallReceiver]. */
    sealed interface InstallEvent {
        data object WaitingForConfirm : InstallEvent
        data object Succeeded : InstallEvent
        data object Cancelled : InstallEvent
        data class Failed(val message: String) : InstallEvent
    }

    private val _events = MutableStateFlow<InstallEvent?>(null)
    val events: StateFlow<InstallEvent?> = _events.asStateFlow()
    internal fun report(e: InstallEvent?) { _events.value = e }

    fun updateFile(ctx: Context) = File(File(ctx.cacheDir, "updates").apply { mkdirs() }, "MVK-update.apk")

    /** Android's one-time "Install unknown apps" switch for MVK. */
    fun canInstall(ctx: Context) = ctx.packageManager.canRequestPackageInstalls()

    /**
     * Downloads [url] to the cache, reporting (bytesSoFar, totalBytes or -1).
     * Then checks the file really is a newer MVK before returning it.
     */
    suspend fun download(ctx: Context, url: String, onProgress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val out = updateFile(ctx)
        val part = File(out.parentFile, "MVK-update.part")
        part.delete()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
        }
        try {
            if (conn.responseCode != 200) error("Download failed (server replied ${conn.responseCode}).")
            val total = conn.contentLengthLong
            var read = 0L
            var lastReported = -1L
            conn.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        // Report about every 256 KB so the UI isn't flooded.
                        if (read - lastReported >= 256 * 1024 || read == total) {
                            lastReported = read
                            onProgress(read, total)
                        }
                    }
                }
            }
            if (total > 0 && read != total) error("Download was interrupted. Try again.")
        } catch (e: Exception) {
            part.delete()
            throw e
        } finally {
            conn.disconnect()
        }
        out.delete()
        if (!part.renameTo(out)) error("Couldn't save the update.")
        verify(ctx, out)
        out
    }

    /** Refuse anything that isn't MVK or isn't newer than what's installed. */
    fun verify(ctx: Context, apk: File) {
        val info = ctx.packageManager.getPackageArchiveInfo(apk.path, 0)
            ?: run { apk.delete(); error("The downloaded file isn't a valid app. Try again.") }
        if (info.packageName != ctx.packageName) {
            apk.delete(); error("The downloaded file isn't MVK.")
        }
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        if (code <= BuildConfig.VERSION_CODE) {
            apk.delete(); error("You already have this version.")
        }
    }

    /** Hands the APK to Android's installer. Results arrive in [InstallReceiver]. */
    fun install(ctx: Context, apk: File) {
        report(null)
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            if (Build.VERSION.SDK_INT >= 31) {
                // Lets Android skip the confirmation when an app updates itself, if it allows it.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val intent = Intent(ctx, InstallReceiver::class.java).setAction(InstallReceiver.ACTION)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val pending = PendingIntent.getBroadcast(ctx, sessionId, intent, flags)
                session.commit(pending.intentSender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }
}

/** Receives the installer's answers and shows Android's confirmation screen when needed. */
class InstallReceiver : BroadcastReceiver() {
    companion object { const val ACTION = "com.seanpereyra.mvk.INSTALL_STATUS" }

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    AppUpdater.report(AppUpdater.InstallEvent.WaitingForConfirm)
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(confirm) }
                        .onFailure { AppUpdater.report(AppUpdater.InstallEvent.Failed("Couldn't open Android's installer.")) }
                } else {
                    AppUpdater.report(AppUpdater.InstallEvent.Failed("Android didn't ask for confirmation. Try again."))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                AppUpdater.updateFile(context).delete()
                AppUpdater.report(AppUpdater.InstallEvent.Succeeded)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> AppUpdater.report(AppUpdater.InstallEvent.Cancelled)
            else -> {
                val reason = when (status) {
                    PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough storage space."
                    PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                        "This update doesn't match the installed app."
                    PackageInstaller.STATUS_FAILURE_BLOCKED -> "Android blocked the install."
                    else -> intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Install failed."
                }
                AppUpdater.report(AppUpdater.InstallEvent.Failed(reason))
            }
        }
    }
}
