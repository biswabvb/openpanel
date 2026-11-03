# OpenPanel (Clean Rebuild of ABCD Panel)

An open-source, de-obfuscated clean rebuild of **ABCD PANEL** (formerly *DJ GAMING VIP INJECTOR*) designed for Android Studio and Termux.

---

## 🔍 Reverse Engineering Analysis Summary

### 1. Target Overview
* **Original App Name:** ABCD PANEL / DJ GAMING VIP INJECTOR (`Theme_DJGAMINGVIPINJECTOR`)
* **Package Name:** `com.abcd.panel`
* **Target Games:**
  * Free Fire MAX (`com.dts.freefiremax`)
  * Free Fire Standard (`com.dts.freefireth`)
  * Free Fire Garena (`com.garena.game.ffi`)

### 2. Tech Stack & Architecture
* **Framework:** Android Native (Java / AndroidX / Material Components).
* **Packer & Obfuscation:** **NP Manager v3.1.47**:
  * `ApkDex2CPro`: Core methods in `MainActivity`, `AuthSystem`, `FloatingOverlayService`, and `ShizukuHelper` were converted from Dalvik bytecode into native ARM64 functions inside `lib/arm64-v8a/libnpdcc.so`.
  * `ApkControlFlowConfusion 8.0`: High-level control flow obfuscation in classes.dex.
* **Privileged Execution:** **Shizuku API v3** (`rikka.shizuku:api:13.1.5` / `rikka.shizuku:provider:13.1.5`).
  * Used to execute elevated shell commands (`sh -c`) with `adb` privileges (UID 2000), bypassing Android 11+ scoped storage restrictions on `/storage/emulated/0/Android/data/`.
* **Floating Window Overlay:** **Android WindowManager** (`SYSTEM_ALERT_WINDOW` / `TYPE_APPLICATION_OVERLAY`).
  * Draggable floating pill HUD widget (`floating_hud_pill.xml`) that renders status, expiration, seller, and active features.
* **Canvas Particles:** Custom view `GlowingTriangleParticles` that renders animated connecting geometric nodes on the canvas.

### 3. How the Original Tool Worked (Mechanism Revealed)
1. **Not a Memory Injector:** The app does not inject `.so` libraries into running game process memory or use ptrace.
2. **File Configuration Injection:**
   * It extracts an embedded configuration file `res/raw/localconfig.json` containing:
     ```json
     {"verAddr":"https://uidbypass.fun/"}
     ```
   * It uses Shizuku shell commands to copy this file directly into the target game's data directory:
     ```bash
     mkdir -p "/storage/emulated/0/Android/data/<package>/files"
     cp -f "<temp_file>" "/storage/emulated/0/Android/data/<package>/files/localConfig.json"
     chmod 777 "/storage/emulated/0/Android/data/<package>/files/localConfig.json"
     ```
3. **Key / Auth System (Removed in this Rebuild):**
   * The original APK forced users through a license screen (`etLicenseKey` and `btnLicenseContinue`).
   * It queried an external PHP endpoint:
     `https://gxpanel.myvipsite.fun/connect?game=FREEFIRE&user_key=<key>&android_id=...`
   * It verified response tokens with an MD5 salt: `Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E`.
   * It also queried a maintenance URL: `https://abcdpanel.myperfectsite.fun/maintenance.php`.
   * **In this clean project:** All remote authentication, licensing locks, tracking, and maintenance checks have been **completely eliminated**. The app is permanently active and fully unlocked.

---

## 📁 Clean Project Structure

```
AbcdPanelClean/
├── build.gradle               # Project-level Gradle configuration
├── settings.gradle            # Repository and project settings
├── gradle.properties          # JVM & AndroidX settings
├── manage_config.sh           # Standalone Termux CLI tool using Shizuku/rish
└── app/
    ├── build.gradle           # App-level dependencies (Shizuku, Material, AppCompat)
    ├── proguard-rules.pro
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── java/com/openpanel/app/
            │   ├── MainActivity.java             # Main dashboard & permissions
            │   ├── FloatingOverlayService.java   # Draggable floating HUD overlay
            │   ├── ShizukuHelper.java            # Shizuku elevated command executor
            │   ├── ConfigManager.java            # Target detection & file installer
            │   └── view/
            │       └── GlowingTriangleParticles.java  # Animated canvas particles
            └── res/
                ├── drawable/                     # Cyber dark UI drawables & buttons
                ├── layout/
                │   ├── activity_main.xml         # Modern dual-tab dashboard
                │   └── floating_hud_pill.xml     # Draggable floating HUD
                ├── raw/
                │   └── localconfig.json          # Target configuration payload
                └── values/                       # Colors, themes, strings
```

---

## 🚀 How to Build & Run

### Option 1: Open in Android Studio
1. Clone or copy `AbcdPanelClean` to your computer.
2. Open **Android Studio** and select **File → Open**, then select the `AbcdPanelClean` directory.
3. Allow Gradle to sync.
4. Connect your Android device or start an emulator.
5. Click **Run (▶)** to build and install the APK.

### Option 2: Termux Direct CLI (Instant Local Use)
If you are running on Termux with Shizuku installed, you can use the bundled CLI script immediately without compiling:

```bash
# Check status of game configurations
./manage_config.sh status

# Install config to Free Fire MAX
./manage_config.sh install com.dts.freefiremax

# Remove config
./manage_config.sh remove com.dts.freefiremax

# Launch game
./manage_config.sh launch com.dts.freefiremax
```
