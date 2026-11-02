# 🎓 Smart Exam Paper Security and Mobile Monitoring System
### VELS Institute of Science, Technology & Advanced Studies (VISTAS)
**Department of Computer Science & Engineering • Final-Year Engineering Capstone Project**

---

## 📌 Project Abstract & Overview

The original system concept used a physical tamper-proof exam box constructed with microcontrollers (Arduino / ESP32), physical solenoid locks, discrete GPS modules, and GSM modems.

In this engineering implementation, **the smartphone itself acts as the hardened physical security device**. The smartphone protects the digital examination question paper vault, continuously tracks its GPS location against the VELS campus geofence, enforces multi-factor authentication (Android Keystore AES-256-GCM + salted SHA-256 PIN + Biometric fingerprint), detects tampering/jamming, buffers audit logs offline in SQLite, and communicates with the Central Examination Controller on the laptop over USB or Wi-Fi.

---

## 📂 Project Directory Framework & Structure

```
Exam box/
├── START_HERE.bat                  # 🚀 ONE-CLICK MASTER LAUNCHER (Run this first!)
├── connect_phone.bat               # 🔌 Interactive USB Phone Controller (Lock/Unlock/Install)
├── clean_project.bat               # 🧹 Cleans temporary caches before zipping to send
├── SmartExamSecurity-debug.apk     # 📲 Pre-compiled Android Debug APK (23.1 MB, Ready to install)
├── server.py                       # 🌐 Python HTTP + ADB USB Remote Bridge API (Port 8080)
├── index.html                      # 🔀 Root redirect to interactive web demo
│
├── web_demo/                       # 💻 Interactive Google Chrome Web Companion
│   ├── index.html                  # Realistic phone emulator + Viva evaluation deck
│   ├── styles.css                  # Cybersecurity Dark Navy & Cyber Cyan theme
│   └── app.js                      # 13 screens logic, Canvas radar sweep, sound synthesizer
│
├── app/                            # 🤖 Android Studio Project Source Code (Kotlin)
│   ├── build.gradle.kts            # Android build configuration (compileSdk 36, targetSdk 35)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions (GPS, Biometric, USB deep-links, actions)
│       │   ├── kotlin/com/smartexambox/security/
│       │   │   ├── MainActivity.kt               # Entry activity + USB remote lock/unlock receiver
│       │   │   ├── Navigation.kt                 # Jetpack Compose navigation graph (13 screens)
│       │   │   ├── data/
│       │   │   │   ├── db/SecurityDatabaseHelper.kt  # Thread-safe SQLite security ledger
│       │   │   │   ├── model/SecurityEvent.kt        # Event models, enums & audit records
│       │   │   │   └── repository/SecurityRepository.kt # StateFlows, CSV export, stats
│       │   │   ├── security/
│       │   │   │   ├── SecurityKeystoreManager.kt    # Android Keystore AES-256-GCM crypto
│       │   │   │   ├── LockoutManager.kt             # 3-strike counter & 60s cooldown
│       │   │   │   ├── RiskScoringEngine.kt          # 0-100 composite threat calculator
│       │   │   │   └── BiometricAuthManager.kt       # BiometricPrompt integration
│       │   │   ├── monitor/
│       │   │   │   ├── LocationMonitor.kt            # GPS geofencing & Haversine distance
│       │   │   │   ├── NetworkConnectivityMonitor.kt # Wi-Fi SSID & connectivity observer
│       │   │   │   └── BatteryMonitor.kt             # Battery level & charging state
│       │   │   ├── sync/
│       │   │   │   └── SyncManager.kt                # WorkManager offline ledger sync
│       │   │   ├── notification/
│       │   │   │   └── SecurityNotificationHelper.kt # High-priority alarm banners
│       │   │   ├── demo/
│       │   │   │   └── DemoSimulationEngine.kt       # 5 examiner viva demonstration scenarios
│       │   │   └── ui/
│       │   │       ├── MainSecurityViewModel.kt      # Shared reactive ViewModel
│       │   │       ├── theme/                        # Navy Background & Cyber Cyan palette
│       │   │       └── screens/                      # All 13 Jetpack Compose screens
│       │   └── res/                                  # Vector drawables, icons, XML strings
│       └── test/                                     # Unit & integration test suite (All passing)
│           └── kotlin/com/smartexambox/security/
│               ├── AuthenticationLogicTest.kt
│               ├── RiskScoringEngineTest.kt
│               ├── GeofenceCalculatorTest.kt
│               └── SecurityEventLoggingTest.kt
│
├── gradle/                         # Gradle 9.1.0 wrapper configuration
├── gradlew.bat                     # Windows Gradle command wrapper
├── README.md                       # Full technical engineering documentation
└── screenshots/                    # High-resolution UI captures for reports
```

---

## ⚡ Quick Start for Your Friend (How to Run in 1 Minute)

### Option 1: Double-Click `START_HERE.bat`
1. Open the project folder on Windows.
2. Double-click **`START_HERE.bat`**.
3. Press **`1`** to launch the interactive demonstration in Google Chrome at `http://localhost:8080`.
4. Press **`2`** to connect a physical Android smartphone via USB cable.

---

### Option 2: Run in Google Chrome (Without Android Studio)
1. Double-click **`server.py`** (or run `python server.py`).
2. Open Chrome and go to: **`http://localhost:8080`**.
3. You will see:
   - The **Smartphone Emulator** running all 13 screens on the left.
   - The **USB Mobile Controller** & **Viva Evaluation Deck** on the right.

---

### Option 3: Connecting Your Phone via USB Cable (Box Replacement Mode)
1. On your phone: Enable **USB Debugging** (`Settings` ➔ `About Phone` ➔ tap `Build Number` 7 times ➔ `Developer Options` ➔ enable `USB Debugging`).
2. Connect your phone to your laptop using a USB data cable.
3. In Chrome or `connect_phone.bat`:
   - Click **"Push & Install APK to Phone"** ➔ Installs the app over USB in seconds!
   - Click **"Launch App on Phone"** ➔ Opens the app on your phone's screen.
   - Click **"Remote Lock Phone Vault"** ➔ Sends the hardware lock signal over USB!
   - Click **"Remote Unlock Phone Vault"** ➔ Unseals the vault on the phone!

---

### Option 4: Open in Android Studio
1. Open **Android Studio** (2024.x or newer).
2. Select **File** ➔ **Open...** ➔ choose the `Exam box` folder.
3. Gradle will sync automatically using JDK 17.
4. Click the green **Run (▶)** button to launch on an emulator or your physical USB phone!

---

## 🔑 Key Credentials & Demo Constants

| Parameter | Value | Description |
| :--- | :--- | :--- |
| **Default Supervisor PIN** | `123456` | 6-digit numeric PIN with salted SHA-256 hash |
| **Admin Master PIN** | `999888` | Emergency supervisor bypass PIN |
| **VELS Campus Center** | `12.969845° N, 80.144412° E` | Main Block, Pallavaram, Chennai |
| **Geofence Radius** | `250 meters` | Permitted perimeter around exam hall |
| **Lockout Trigger** | `3 failed PINs` | Triggers 60-second hardware cooldown |
| **Encryption Standard** | `AES-256-GCM` | Hardware-backed Android Keystore Master Key |
| **Threat Risk Score** | `0 - 100` | Composite sensor index (Green <30, Red ≥70) |

---

## 🎤 Viva Examiner Q&A Cheat Sheet

**Q1: How does this project replace the physical IoT exam box?**  
*Answer:* The smartphone hardware acts as the physical box. Its built-in GNSS GPS chip replaces discrete NEO-6M modules; its Wi-Fi/Cellular baseband replaces GSM shields; its biometric fingerprint and touch display replace physical keypads and solenoid latches; and the hardware-isolated Android Keystore replaces discrete EEPROMs.

**Q2: What happens if an attacker tries to tamper with or open the phone outside the exam hall?**  
*Answer:* The location monitor continuously runs Haversine calculations against the VELS perimeter (`12.9698° N, 80.1444° E`). If the phone exits the 250m radius, the Risk Scoring Engine jumps from nominal (<20) to Critical (>80), triggers high-priority alarm banners, and freezes the Keystore decryption routine.

**Q3: How are the question paper files encrypted?**  
*Answer:* Files are encrypted using AES-256 in Galois/Counter Mode (GCM) with 128-bit authentication tags. Keys are generated inside the device's hardware Secure Element / StrongBox via Android Keystore Provider and never leave the hardware sandbox.

**Q4: What happens if there is network jamming or no Wi-Fi?**  
*Answer:* The app is fully offline-first. All security audit events are stored in a thread-safe SQLite database on the device. When connectivity restores, Android WorkManager flushes the buffered records to the central server.
