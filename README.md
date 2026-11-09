# MVK — May Voucher Ka?

Android app for stocking up school Wi-Fi voucher codes. Created by **Sean Pereyra**.

## Features
- **Library** – 2-column grid of your vouchers, styled like the paper slip. Tap one to copy, "Copy & open Wi-Fi", mark as used, edit or delete. Available / Used tabs, plus a "+" to add one by hand.
- **Scan** – point the camera at a voucher slip; the code, slot number (e.g. `[153]`) and time (e.g. `6h`) are read on-device and shown for you to confirm before saving. Flashlight toggle and "Type it in" fallback.
- **Random** – press **GO!** for a random 5-character code (A–Z, a–z, 0–9). Tap to copy, "Copy & open Wi-Fi", or "It worked!" to save it.
- **Settings** (gear, top-right) – Themes (System / Dark / AMOLED / Light + accent colors), Check for updates, About.

Everything stays on the phone. The camera is only used for reading codes; the internet is only used by "Check for updates".

## Build & install
1. Install **Android Studio** (latest stable).
2. *File → Open* and pick this `MVK` folder. Let Gradle sync (first time downloads ~1 GB).
3. Plug in your phone with USB debugging on, then press **Run ▶**.
4. For a shareable APK: *Build → Build App Bundle(s) / APK(s) → Build APK(s)*. The file appears under `app/build/outputs/apk/`.

If Android Studio offers to upgrade Gradle / AGP / libraries, it's safe to accept.

Requires Android 8.0 (API 26) or newer.

## Setting up "Check for updates"
1. Push this project to a GitHub repo.
2. In `app/src/main/java/com/seanpereyra/mvk/data/UpdateChecker.kt`, set `UPDATE_URL` to
   `https://raw.githubusercontent.com/<you>/MVK/main/version.json`.
3. When you release a new version: bump `versionCode` / `versionName` in `app/build.gradle.kts`,
   upload the APK to GitHub Releases, then bump the same numbers in `version.json`.

Until the URL is set, the button simply says the update server isn't set up yet.

## Project layout
```
app/src/main/java/com/seanpereyra/mvk/
├─ MainActivity.kt            tabs + bottom bar
├─ data/                      Voucher model, local storage, settings, update checker
├─ scan/VoucherParser.kt      turns camera text into code / slot / time
└─ ui/
   ├─ theme/Theme.kt          colors, themes, fonts
   ├─ components/             header + logo, bottom bar, voucher ticket, add/edit form
   └─ screens/                Library, Scan, Randomizer, Settings
```

## Fonts
Inter Display (headings), Poppins (UI text), DejaVu Sans Mono Bold (voucher codes, so 0/O and l/I are easy to tell apart). All are free, open-licensed fonts bundled in `res/font`.

## Swapping in your real logo
The header logo is drawn in `MvkLogo()` in `ui/components/Components.kt`. The launcher icon is `res/drawable/ic_launcher_foreground.xml`. Replace either when your final MVK logo is ready.
