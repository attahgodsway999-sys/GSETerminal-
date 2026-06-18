# GSE Terminal — Android APK Build Guide

Ghana Stock Exchange live terminal, packaged as a native Android WebView app.

---

## Project Structure

```
GSETerminal/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/
│   │   │   └── index.html              ← Your GSE Terminal web app
│   │   ├── java/com/gseterminal/app/
│   │   │   ├── GSEApplication.kt       ← App class (WebView debug toggle)
│   │   │   ├── SplashActivity.kt       ← Launch screen (1.8 s)
│   │   │   ├── MainActivity.kt         ← WebView host
│   │   │   └── WebAppInterface.kt      ← JS ↔ Native bridge
│   │   └── res/
│   │       ├── layout/
│   │       │   ├── activity_splash.xml
│   │       │   └── activity_main.xml
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   ├── colors.xml
│   │       │   └── themes.xml
│   │       ├── xml/
│   │       │   ├── network_security_config.xml
│   │       │   ├── backup_rules.xml
│   │       │   └── data_extraction_rules.xml
│   │       └── mipmap-*/               ← Launcher icons (all densities)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml              ← Version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties.template           ← Copy → local.properties (not in git)
└── .gitignore
```

---

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog 2023.1.1+ | https://developer.android.com/studio |
| JDK | 17 (bundled with AS) | Included |
| Android SDK | API 34 | Via SDK Manager |
| Build Tools | 34.0.0 | Via SDK Manager |

---

## Step 1 — Open the project

1. Launch **Android Studio**
2. **File → Open** → select the `GSETerminal/` folder
3. Wait for the IDE to index files (~30 s)

---

## Step 2 — Set up local.properties

```bash
cp local.properties.template local.properties
```

Edit `local.properties`:
```properties
sdk.dir=/Users/YOUR_NAME/Library/Android/sdk    # macOS
# sdk.dir=C:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk   # Windows
```

> `local.properties` is git-ignored. Never commit it.

---

## Step 3 — Gradle Sync

1. Android Studio shows a banner: **"Gradle files have changed"**
2. Click **Sync Now** (or go to **File → Sync Project with Gradle Files**)
3. First sync downloads ~500 MB of dependencies — this is normal
4. If sync fails, check: **File → Project Structure → SDK Location**

---

## Step 4 — Build Debug APK

### Via Android Studio UI:
**Build → Build Bundle(s) / APK(s) → Build APK(s)**

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Via Terminal:
```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

---

## Step 5 — Run on device / emulator

### On a real device:
1. Enable **Developer Options** on your phone (tap Build Number 7×)
2. Enable **USB Debugging**
3. Connect via USB
4. Click ▶ **Run** in Android Studio

### On emulator:
1. **Tools → Device Manager → Create Virtual Device**
2. Choose Pixel 6, API 34
3. Click ▶ **Run**

---

## Step 6 — Build Release APK (for distribution)

### 6a. Create a keystore (one time only)

```bash
keytool -genkey -v \
  -keystore keystore/gse_release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias gse_key
```

Store the keystore file safely. If you lose it, you cannot update your app on Play Store.

### 6b. Add signing config to local.properties

```properties
KEYSTORE_FILE=../keystore/gse_release.jks
KEY_ALIAS=gse_key
KEY_PASSWORD=your_key_password
STORE_PASSWORD=your_store_password
```

### 6c. Build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## API Domains — Network Security

The following domains are explicitly trusted in `network_security_config.xml`:

| Domain | Purpose |
|--------|---------|
| `dev.kwayisi.org` | GSE live price API |
| `api.rss2json.com` | RSS-to-JSON proxy for news |
| `api.anthropic.com` | Claude AI news fallback |
| `cdnjs.cloudflare.com` | Font Awesome CSS |
| `fonts.googleapis.com` | Google Fonts (Inter, JetBrains Mono) |
| `fonts.gstatic.com` | Google Font files |
| `logo.clearbit.com` | Company logos |
| `www.google.com` | Favicon fallback |

All traffic is **HTTPS only** — cleartext is blocked globally.

---

## JavaScript ↔ Native Bridge

Your HTML can call native Android features:

```javascript
// Detect Android app context
if (window.AndroidBridge && window.AndroidBridge.isAndroid()) {
    // Running in the native app
}

// Show a native toast
window.AndroidBridge.showToast("Price updated!");

// Open a link in the system browser
window.AndroidBridge.openUrl("https://gse.com.gh");

// Share a stock price
window.AndroidBridge.shareText("MTNGH is trading at ₵6.39 on the GSE");

// Get app version
const version = window.AndroidBridge.getAppVersion();
```

---

## Debugging

### Inspect the WebView from desktop Chrome:
1. Build and install the **debug** APK
2. Open `chrome://inspect` in desktop Chrome
3. Your device WebView appears — click **inspect**
4. Full DevTools: Console, Network, Sources, Storage

### Logcat JS console:
All `console.log()` calls from your JS are forwarded to Logcat tagged `GSE_JS`:
```bash
adb logcat -s GSE_JS
```

---

## Common Issues & Fixes

| Problem | Cause | Fix |
|---------|-------|-----|
| Blank white screen on launch | JS error in HTML | Check `chrome://inspect` console |
| Network calls fail silently | Missing INTERNET permission | Already in Manifest — verify Gradle sync |
| `net::ERR_CLEARTEXT_NOT_PERMITTED` | HTTP call to API | Add domain to `network_security_config.xml` |
| localStorage empty after reinstall | WebView storage cleared on uninstall | Expected behaviour |
| Back button exits app immediately | WebView history empty | Handled — loads local HTML which has JS routing |
| Gradle sync fails: SDK not found | `sdk.dir` wrong in local.properties | Update path to your actual SDK location |
| `duplicate class` error | Dependency conflict | Run `./gradlew dependencies` to diagnose |

---

## Tech Stack

- **Language:** Kotlin 1.9
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Architecture:** Single-Activity WebView wrapper
- **Web app:** Vanilla HTML/CSS/JS (no framework)
- **Data APIs:** kwayisi.org GSE, rss2json, Anthropic Claude
